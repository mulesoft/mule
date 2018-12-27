/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.ImmutableMap.of;
import static org.mule.runtime.api.exception.MuleException.INFO_ALREADY_LOGGED_KEY;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.create;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

/**
 * {@link SourcePolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with source policies.
 *
 * @since 4.0
 */
public class CompositeSourcePolicy
    extends AbstractCompositePolicy<SourcePolicyParametersTransformer, Processor> implements SourcePolicy, Disposable {

  private static final String POLICY_SOURCE_ORIGINAL_FAILURE_RESPONSE_PARAMETERS =
      "policy.source.originalFailureResponseParameters";
  private static final String POLICY_SOURCE_ORIGINAL_RESPONSE_PARAMETERS = "policy.source.originalResponseParameters";

  public static final String POLICY_SOURCE_PARAMETERS_PROCESSOR = "policy.source.parametersProcessor";

  public static final String POLICY_SOURCE_CALLER_SINK = "policy.source.callerSink";

  private static final Logger LOGGER = getLogger(CompositeSourcePolicy.class);

  private final reactor.core.Disposable fluxSubscription;
  private final FluxSink<CoreEvent> policySink;

  private final SourcePolicyProcessorFactory sourcePolicyProcessorFactory;
  private final ReactiveProcessor flowExecutionProcessor;

  /**
   * Creates a new source policies composed by several {@link Policy} that will be chain together.
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
    super(parameterizedPolicies, sourcePolicyParametersTransformer);
    this.flowExecutionProcessor = flowExecutionProcessor;
    this.sourcePolicyProcessorFactory = sourcePolicyProcessorFactory;

    AtomicReference<FluxSink<CoreEvent>> sinkRef = new AtomicReference<>();

    Flux<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> policyFlux =
        Flux.<CoreEvent>create(sink -> sinkRef.set(sink))
            .transform(getExecutionProcessor())
            .map(policiesResultEvent -> {
              final Map<String, Object> originalResponseParameters = ((InternalEvent) policiesResultEvent)
                  .getInternalParameter(POLICY_SOURCE_ORIGINAL_RESPONSE_PARAMETERS);
              Supplier<Map<String, Object>> responseParameters = () -> getParametersTransformer()
                  .map(parametersTransformer -> concatMaps(originalResponseParameters, parametersTransformer
                      .fromMessageToSuccessResponseParameters(policiesResultEvent.getMessage())))
                  .orElse(originalResponseParameters);
              return right(SourcePolicyFailureResult.class,
                           new SourcePolicySuccessResult(policiesResultEvent, responseParameters,
                                                         ((InternalEvent) policiesResultEvent)
                                                             .getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR)));
            })
            .doOnNext(result -> logSourcePolicySuccessfullResult(result.getRight()))
            .doOnError(e -> !(e instanceof FlowExecutionException || e instanceof MessagingException),
                       e -> LOGGER.error(e.getMessage(), e))
            .onErrorContinue(MessagingException.class, (t, e) -> {
              final MessagingException me = (MessagingException) t;

              Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result =
                  left(new SourcePolicyFailureResult(me, resolveErrorResponseParameters(me)), SourcePolicySuccessResult.class);

              if (!(me instanceof FlowExecutionException)) {
                logSourcePolicyFailureResult(result.getLeft());
              }

              final InternalEvent event = (InternalEvent) me.getEvent();
              if (!event.getContext().isComplete()) {
                event.getContext().error(me);
              }

              ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) event
                  .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
            })
            .doOnNext(result -> result.apply(spfr -> {
              final InternalEvent event = (InternalEvent) spfr.getMessagingException().getEvent();
              if (!event.getContext().isComplete()) {
                event.getContext().error(spfr.getMessagingException());
              }
              ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) event
                  .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
            }, spsr -> {
              final InternalEvent event = (InternalEvent) spsr.getResult();
              if (!event.getContext().isComplete()) {
                event.getContext().success(event);
              }
              ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) event
                  .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
            }));

    fluxSubscription = policyFlux.subscribe();
    policySink = sinkRef.get();
  }

  /**
   * Executes the flow.
   * <p>
   * If there's a {@link SourcePolicyParametersTransformer} provided then it will use it to convert the source response or source
   * failure response from the parameters back to a {@link Message} that can be routed through the policy chain which later will
   * be convert back to response or failure response parameters thus allowing the policy chain to modify the response.. That
   * message will be the result of the next-operation of the policy.
   * <p>
   * If no {@link SourcePolicyParametersTransformer} is provided, then the same response from the flow is going to be routed as
   * response of the next-operation of the policy chain. In this case, the same response from the flow is going to be used to
   * generate the response or failure response for the source so the policy chain is not going to be able to modify the response
   * sent by the source.
   * <p>
   * When the flow execution fails, it will create a {@link FlowExecutionException} instead of a regular
   * {@link MessagingException} to signal that the failure was through the the flow exception and not the policy logic.
   */
  @Override
  protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub) {
    return from(eventPub)
        .transform(flowExecutionProcessor)
        .map(flowExecutionResponse -> {
          MessageSourceResponseParametersProcessor parametersProcessor =
              ((InternalEvent) flowExecutionResponse).getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR);

          Map<String, Object> originalResponseParameters =
              parametersProcessor.getSuccessfulExecutionResponseParametersFunction().apply(flowExecutionResponse);

          Message message = getParametersTransformer()
              .map(parametersTransformer -> parametersTransformer
                  .fromSuccessResponseParametersToMessage(originalResponseParameters))
              .orElseGet(flowExecutionResponse::getMessage);

          return InternalEvent.builder(flowExecutionResponse)
              .message(message)
              .addInternalParameter(POLICY_SOURCE_ORIGINAL_RESPONSE_PARAMETERS, originalResponseParameters)
              .build();
        })
        .cast(CoreEvent.class)
        .onErrorMap(MessagingException.class, messagingException -> {
          MessageSourceResponseParametersProcessor parametersProcessor =
              ((InternalEvent) messagingException.getEvent()).getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR);

          Map<String, Object> originalFailureResponseParameters =
              parametersProcessor.getFailedExecutionResponseParametersFunction().apply(messagingException.getEvent());

          Message message = getParametersTransformer()
              .map(parametersTransformer -> parametersTransformer
                  .fromFailureResponseParametersToMessage(originalFailureResponseParameters))
              .orElse(messagingException.getEvent().getMessage());

          MessagingException flowExecutionException =
              new FlowExecutionException(InternalEvent.builder(messagingException.getEvent())
                  .message(message)
                  .addInternalParameter(POLICY_SOURCE_ORIGINAL_FAILURE_RESPONSE_PARAMETERS, originalFailureResponseParameters)
                  .build(),
                                         messagingException.getCause(),
                                         messagingException.getFailingComponent());
          if (messagingException.getInfo().containsKey(INFO_ALREADY_LOGGED_KEY)) {
            flowExecutionException.addInfo(INFO_ALREADY_LOGGED_KEY,
                                           messagingException.getInfo().get(INFO_ALREADY_LOGGED_KEY));
          }
          return flowExecutionException;
        })
        .doOnError(e -> !(e instanceof MessagingException), e -> LOGGER.error(e.getMessage(), e));
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
   * @return a {@link Publisher} that emits {@link SourcePolicySuccessResult} which contains the response parameters and the
   *         result event of the execution or a {@link SourcePolicyFailureResult} which contains the failure response parameters
   *         and the {@link MessagingException} thrown by the policy chain execution when processing completes.
   * @throws Exception if there was an unexpected failure thrown by executing the chain.
   */
  @Override
  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent,
                                                                                         MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    return create(callerSink -> {
      policySink.next(quickCopy(sourceEvent, of(POLICY_SOURCE_PARAMETERS_PROCESSOR, messageSourceResponseParametersProcessor,
                                                POLICY_SOURCE_CALLER_SINK, callerSink)));
    });
  }

  protected Supplier<Map<String, Object>> resolveErrorResponseParameters(MessagingException e) {
    final Map<String, Object> originalFailureResponseParameters =
        ((InternalEvent) e.getEvent()).getInternalParameter(POLICY_SOURCE_ORIGINAL_FAILURE_RESPONSE_PARAMETERS);

    return () -> getParametersTransformer()
        .map(parametersTransformer -> concatMaps(originalFailureResponseParameters,
                                                 parametersTransformer
                                                     .fromMessageToErrorResponseParameters(e.getEvent().getMessage())))
        .orElse(originalFailureResponseParameters);
  }

  private Map<String, Object> concatMaps(Map<String, Object> originalResponseParameters,
                                         Map<String, Object> policyResponseParameters) {
    if (originalResponseParameters == null) {
      return policyResponseParameters;
    } else {
      Map<String, Object> concatMap = new HashMap<>();
      concatMap.putAll(originalResponseParameters);
      concatMap.putAll(policyResponseParameters);
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
    policySink.complete();
    fluxSubscription.dispose();
  }
}
