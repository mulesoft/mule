/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.util.collection.SmallMap.copy;
import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.from;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * {@link SourcePolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with source policies.
 *
 * @since 4.0
 */
public class CompositeSourcePolicy
    extends AbstractCompositePolicy<SourcePolicyParametersTransformer> implements SourcePolicy, Disposable, DeferredDisposable {

  private static final Logger LOGGER = getLogger(CompositeSourcePolicy.class);

  private final CommonSourcePolicy commonPolicy;
  private final SourcePolicyProcessorFactory sourcePolicyProcessorFactory;
  private final ReactiveProcessor flowExecutionProcessor;
  private final Optional<Function<MessagingException, MessagingException>> resolver;
  private final PolicyTraceLogger policyTraceLogger = new PolicyTraceLogger();

  /**
   * Creates a new source policy composed by several {@link Policy} that will be chain together.
   *
   * @param parameterizedPolicies the list of policies to use in this composite policy.
   * @param flowExecutionProcessor the operation that executes the flow
   * @param sourcePolicyParametersTransformer a transformer from a source response parameters to a message and vice versa
   * @param sourcePolicyProcessorFactory factory to create a {@link Processor} from each {@link Policy}
   * @param resolver a mapper to update the eventual errors in source policy
   */
  public CompositeSourcePolicy(List<Policy> parameterizedPolicies,
                               ReactiveProcessor flowExecutionProcessor,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               SourcePolicyProcessorFactory sourcePolicyProcessorFactory,
                               Function<MessagingException, MessagingException> resolver) {
    super(parameterizedPolicies, sourcePolicyParametersTransformer);
    this.flowExecutionProcessor = flowExecutionProcessor;
    this.sourcePolicyProcessorFactory = sourcePolicyProcessorFactory;
    this.resolver = ofNullable(resolver);
    initProcessor();
    this.commonPolicy = new CommonSourcePolicy(new SourceWithPoliciesFluxObjectFactory(this));
  }

  @Override
  protected ReactiveProcessor getPolicyProcessor() {
    return getLastPolicy().getPolicyChain().getProcessingStrategy().onPipeline(super.getPolicyProcessor());
  }

  private static final class SourceWithPoliciesFluxObjectFactory implements Supplier<FluxSink<CoreEvent>> {

    private final Reference<CompositeSourcePolicy> compositeSourcePolicy;
    private final PolicyTraceLogger policyTraceLogger = new PolicyTraceLogger();

    public SourceWithPoliciesFluxObjectFactory(CompositeSourcePolicy compositeSourcePolicy) {
      // Avoid instances of this class from preventing the policy from being gc'd
      // Break the circular reference between policy-sinkFactory-flux that may cause memory leaks in the policies caches
      this.compositeSourcePolicy = new WeakReference<>(compositeSourcePolicy);
    }

    @Override
    public FluxSink<CoreEvent> get() {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();

      Flux<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> policyFlux =
          sinkRef.flux()
              .transform(compositeSourcePolicy.get().getExecutionProcessor())
              .map(policiesResultEvent -> {
                SourcePolicyContext ctx = from(policiesResultEvent);
                return right(SourcePolicyFailureResult.class,
                             new SourcePolicySuccessResult(policiesResultEvent,
                                                           resolveSuccessResponseParameters(policiesResultEvent, ctx),
                                                           ctx.getResponseParametersProcessor()));
              })
              .doOnNext(result -> {
                compositeSourcePolicy.get().commonPolicy.finishFlowProcessing(result.getRight().getResult(), result);
              })
              .doOnError(e -> !(e instanceof MessagingException), e -> LOGGER.error(e.getMessage(), e))
              .onErrorContinue(MessagingException.class, (t, e) -> {
                MessagingException me = (MessagingException) t;
                SourcePolicyContext ctx = from(me.getEvent());

                Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result =
                    left(new SourcePolicyFailureResult(me, resolveErrorResponseParameters(me, ctx)),
                         SourcePolicySuccessResult.class);

                policyTraceLogger.logSourcePolicyFailureResult(result.getLeft());

                compositeSourcePolicy.get().commonPolicy.finishFlowProcessing(me.getEvent(), result, me, ctx);
              });

      policyFlux.subscribe(null, e -> LOGGER.error("Exception reached subscriber for " + toString(), e));

      return sinkRef.getFluxSink();
    }

    private Supplier<Map<String, Object>> resolveSuccessResponseParameters(CoreEvent policiesResultEvent,
                                                                           SourcePolicyContext ctx) {
      final Map<String, Object> originalResponseParameters = ctx.getOriginalResponseParameters();

      return () -> compositeSourcePolicy.get().getParametersTransformer()
          .map(parametersTransformer -> concatMaps(originalResponseParameters,
                                                   parametersTransformer
                                                       .fromMessageToSuccessResponseParameters(policiesResultEvent
                                                           .getMessage())))
          .orElse(originalResponseParameters);
    }

    private Supplier<Map<String, Object>> resolveErrorResponseParameters(MessagingException e, SourcePolicyContext ctx) {
      final Map<String, Object> originalFailureResponseParameters = ctx.getOriginalFailureResponseParameters();

      return () -> compositeSourcePolicy.get().getParametersTransformer()
          .map(parametersTransformer -> concatMaps(originalFailureResponseParameters,
                                                   parametersTransformer
                                                       .fromMessageToErrorResponseParameters(e.getEvent().getMessage())))
          .orElse(originalFailureResponseParameters);
    }
  }

  /**
   * Executes the flow.
   * <p>
   * If there's a {@link SourcePolicyParametersTransformer} provided then it will use it to convert the source response or source
   * failure response from the parameters back to a {@link Message} that can be routed through the policy chain which later will
   * be convert back to response or failure response parameters thus allowing the policy chain to modify the response. That
   * message will be the result of the next-operation of the policy.
   * <p>
   * If no {@link SourcePolicyParametersTransformer} is provided, then the same response from the flow is going to be routed as
   * response of the next-operation of the policy chain. In this case, the same response from the flow is going to be used to
   * generate the response or failure response for the source so the policy chain is not going to be able to modify the response
   * sent by the source.
   */
  @Override
  protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub) {
    final Optional<SourcePolicyParametersTransformer> parametersTransformer = getParametersTransformer();
    final Function<MessagingException, MessagingException> errorResolver = resolver.orElse(identity());

    return from(eventPub)
        .doOnNext(e -> SourcePolicyContext.from(e).setParametersTransformer(parametersTransformer))
        .transform(flowExecutionProcessor)
        .map(flowExecutionResponse -> {
          try {
            return new PolicyEventMapper().onFlowFinish(flowExecutionResponse, parametersTransformer);
          } catch (MessagingException e) {
            throw propagateWrappingFatal(errorResolver.apply(e));
          }
        });
  }

  /**
   * Always return the policy execution / flow execution result so the next policy executes with the modified version of the
   * wrapped policy / flow.
   */
  @Override
  protected Publisher<CoreEvent> applyPolicy(Policy policy, ReactiveProcessor nextProcessor, Publisher<CoreEvent> eventPub) {
    final ReactiveProcessor createSourcePolicy = sourcePolicyProcessorFactory.createSourcePolicy(policy, nextProcessor);
    return from(eventPub)
        .doOnNext(event -> policyTraceLogger.logSourcePolicyStart(policy, event))
        .transform(createSourcePolicy)
        .doOnNext(event -> policyTraceLogger.logSourcePolicyEnd(policy, event));
  }

  /**
   * Process the set of policies.
   * <p>
   * When there's a {@link SourcePolicyParametersTransformer} then the final set of parameters to be sent by the response function
   * and the error response function will be calculated based on the output of the policy chain. If there's no
   * {@link SourcePolicyParametersTransformer} then those parameters will be exactly the one defined by the message source.
   *
   * @param sourceEvent the event generated from the source.
   */
  @Override
  public void process(CoreEvent sourceEvent,
                      MessageSourceResponseParametersProcessor respParamProcessor,
                      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {
    commonPolicy.process(sourceEvent, respParamProcessor, callback);
  }

  private static Map<String, Object> concatMaps(Map<String, Object> originalResponseParameters,
                                                Map<String, Object> policyResponseParameters) {
    if (originalResponseParameters == null) {
      return policyResponseParameters;
    } else {
      Map<String, Object> concatMap = copy(originalResponseParameters);
      policyResponseParameters.forEach((k, v) -> concatMap.merge(k, v, (v1, v2) -> v2));
      return concatMap;
    }
  }

  @Override
  public void dispose() {
    commonPolicy.dispose();
  }

  @Override
  public Disposable deferredDispose() {
    return commonPolicy.deferredDispose();
  }
}
