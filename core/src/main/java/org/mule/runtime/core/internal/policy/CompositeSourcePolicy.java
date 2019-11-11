/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
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
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.HashMap;
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
    extends AbstractCompositePolicy<SourcePolicyParametersTransformer> implements SourcePolicy, Disposable {

  private static final Logger LOGGER = getLogger(CompositeSourcePolicy.class);

  public static final String POLICY_SOURCE_ORIGINAL_FAILURE_RESPONSE_PARAMETERS =
      "policy.source.originalFailureResponseParameters";
  public static final String POLICY_SOURCE_ORIGINAL_RESPONSE_PARAMETERS = "policy.source.originalResponseParameters";

  private final CommonSourcePolicy commonPolicy;

  private final SourcePolicyProcessorFactory sourcePolicyProcessorFactory;
  private final ReactiveProcessor flowExecutionProcessor;

  private final PolicyEventMapper policyEventMapper;
  private final Optional<Function<MessagingException, MessagingException>> resolver;

  /**
   * Creates a new source policy composed by several {@link Policy} that will be chain together.
   *
   * @param parameterizedPolicies the list of policies to use in this composite policy.
   * @param flowExecutionProcessor the operation that executes the flow
   * @param sourcePolicyParametersTransformer a transformer from a source response parameters to a message and vice versa
   * @param sourcePolicyProcessorFactory factory to create a {@link Processor} from each {@link Policy}
   */
  public CompositeSourcePolicy(List<Policy> parameterizedPolicies,
                               ReactiveProcessor flowExecutionProcessor,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               SourcePolicyProcessorFactory sourcePolicyProcessorFactory) {
    this(parameterizedPolicies, flowExecutionProcessor, sourcePolicyParametersTransformer, sourcePolicyProcessorFactory, null);
  }

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
    this.commonPolicy = new CommonSourcePolicy(new SourceWithPoliciesFluxObjectFactory(), sourcePolicyParametersTransformer);
    this.policyEventMapper = new PolicyEventMapper();
    this.resolver = ofNullable(resolver);
  }

  private final class SourceWithPoliciesFluxObjectFactory implements Supplier<FluxSink<CoreEvent>> {

    @Override
    public FluxSink<CoreEvent> get() {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();

      Flux<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> policyFlux =
          Flux.create(sinkRef)
              .transform(getExecutionProcessor())
              .map(policiesResultEvent -> right(SourcePolicyFailureResult.class,
                                                new SourcePolicySuccessResult(policiesResultEvent,
                                                                              resolveSuccessResponseParameters(policiesResultEvent),
                                                                              commonPolicy
                                                                                  .getResponseParamsProcessor(policiesResultEvent))))
              .doOnNext(result -> {
                logSourcePolicySuccessfullResult(result.getRight());

                commonPolicy.finishFlowProcessing(result.getRight().getResult(), result);
              })
              .doOnError(e -> !(e instanceof MessagingException), e -> LOGGER.error(e.getMessage(), e))
              .onErrorContinue(MessagingException.class, (t, e) -> {
                MessagingException me = (MessagingException) t;

                Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result =
                    left(new SourcePolicyFailureResult(me, resolveErrorResponseParameters(me)), SourcePolicySuccessResult.class);

                logSourcePolicyFailureResult(result.getLeft());

                commonPolicy.finishFlowProcessing(me.getEvent(), result, me);
              });

      policyFlux.subscribe();
      return sinkRef.getFluxSink();
    }

    private Supplier<Map<String, Object>> resolveSuccessResponseParameters(CoreEvent policiesResultEvent) {
      final Map<String, Object> originalResponseParameters = ((InternalEvent) policiesResultEvent)
          .getInternalParameter(POLICY_SOURCE_ORIGINAL_RESPONSE_PARAMETERS);

      return () -> getParametersTransformer()
          .map(parametersTransformer -> concatMaps(originalResponseParameters,
                                                   parametersTransformer
                                                       .fromMessageToSuccessResponseParameters(policiesResultEvent
                                                           .getMessage())))
          .orElse(originalResponseParameters);
    }

    private Supplier<Map<String, Object>> resolveErrorResponseParameters(MessagingException e) {
      final Map<String, Object> originalFailureResponseParameters =
          ((InternalEvent) e.getEvent()).getInternalParameter(POLICY_SOURCE_ORIGINAL_FAILURE_RESPONSE_PARAMETERS);

      return () -> getParametersTransformer()
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
  protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub, Policy lastPolicy) {

    return from(eventPub)
        .transform(flowExecutionProcessor)
        .map(flowExecutionResponse -> {
          try {
            return policyEventMapper.onFlowFinish(flowExecutionResponse, getParametersTransformer());
          } catch (MessagingException e) {
            throw Exceptions.propagateWrappingFatal(resolver.orElse(exc -> exc).apply(e));
          }
        })
        .onErrorContinue(MessagingException.class, (error, v) -> {

          PolicyNotificationHelper notificationHelper =
              new PolicyNotificationHelper(lastPolicy.getPolicyChain().getMuleContext().getNotificationManager(),
                                           lastPolicy.getPolicyId(),
                                           lastPolicy.getPolicyChain());

          new OnExecuteNextErrorConsumer(event -> policyEventMapper.onFlowError(event, lastPolicy.getPolicyId(),
                                                                                getParametersTransformer()),
                                         notificationHelper, lastPolicy.getPolicyChain().getLocation())
                                             .accept(error);
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
        .doOnNext(s -> logEvent(getCoreEventId(s), getPolicyName(policy), () -> getCoreEventAttributesAsString(s),
                                "Starting Policy "))
        .transform(createSourcePolicy)
        .doOnNext(responseEvent -> logEvent(getCoreEventId(responseEvent), getPolicyName(policy),
                                            () -> getCoreEventAttributesAsString(responseEvent), "At the end of the Policy "));
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

  private Map<String, Object> concatMaps(Map<String, Object> originalResponseParameters,
                                         Map<String, Object> policyResponseParameters) {
    if (originalResponseParameters == null) {
      return policyResponseParameters;
    } else {
      Map<String, Object> concatMap = new HashMap<>(originalResponseParameters);
      policyResponseParameters.forEach((k, v) -> concatMap.merge(k, v, (v1, v2) -> v2));
      return concatMap;
    }
  }

  private void logEvent(String eventId, String policyName, Supplier<String> message, String startingMessage) {
    if (LOGGER.isTraceEnabled()) {
      // TODO Remove event id when first policy generates it. MULE-14455
      LOGGER.trace("Event Id: " + eventId + ".\n" + startingMessage + policyName + "\n" + message.get());
    }
  }

  private String getCoreEventId(CoreEvent event) {
    return event.getContext().getId();
  }

  private String getCoreEventAttributesAsString(CoreEvent event) {
    if (event.getMessage() == null || event.getMessage().getAttributes() == null
        || event.getMessage().getAttributes().getValue() == null) {
      return "";
    }
    return event.getMessage().getAttributes().getValue().toString();
  }

  private String getPolicyName(Policy policy) {
    return policy.getPolicyId();
  }

  private void logSourcePolicySuccessfullResult(SourcePolicySuccessResult result) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Event id: " + result.getResult().getContext().getId() + "\nFinished processing. \n" +
          getCoreEventAttributesAsString(result.getResult()));
    }
  }

  private void logSourcePolicyFailureResult(SourcePolicyFailureResult result) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Event id: " + result.getMessagingException().getEvent().getContext().getId()
          + "\nFinished processing with failure. \n" +
          "Error message: " + result.getMessagingException().getMessage());
    }
  }

  @Override
  public void dispose() {
    commonPolicy.dispose();
  }
}
