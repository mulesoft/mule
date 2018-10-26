/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.empty;
import static org.mule.runtime.api.exception.MuleException.INFO_ALREADY_LOGGED_KEY;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.MessageProcessors;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link SourcePolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with source policies.
 *
 * @since 4.0
 */
public class CompositeSourcePolicy extends
    AbstractCompositePolicy<SourcePolicyParametersTransformer, MessageSourceResponseParametersProcessor> implements SourcePolicy {

  private static final Logger LOGGER = getLogger(CompositeSourcePolicy.class);

  private final Processor flowExecutionProcessor;
  private final SourcePolicyProcessorFactory sourcePolicyProcessorFactory;
  private Map<String, Object> originalResponseParameters;
  private Map<String, Object> originalFailureResponseParameters;

  /**
   * Creates a new source policies composed by several {@link Policy} that will be chain together.
   *
   * @param parameterizedPolicies the list of policies to use in this composite policy.
   * @param sourcePolicyParametersTransformer a transformer from a source response parameters to a message and vice versa
   * @param sourcePolicyProcessorFactory factory to create a {@link Processor} from each {@link Policy}
   * @param flowExecutionProcessor the operation that executes the flow
   * @param messageSourceResponseParametersProcessor processor that gives access to the set of parameters to be sent originally by
   *        the source
   */
  public CompositeSourcePolicy(List<Policy> parameterizedPolicies,
                               Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer,
                               SourcePolicyProcessorFactory sourcePolicyProcessorFactory, Processor flowExecutionProcessor,
                               MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    super(parameterizedPolicies, sourcePolicyParametersTransformer, messageSourceResponseParametersProcessor);
    this.sourcePolicyProcessorFactory = sourcePolicyProcessorFactory;
    this.flowExecutionProcessor = flowExecutionProcessor;
  }

  /**
   * Executes the flow.
   *
   * If there's a {@link SourcePolicyParametersTransformer} provided then it will use it to convert the source response or source
   * failure response from the parameters back to a {@link Message} that can be routed through the policy chain which later will
   * be convert back to response or failure response parameters thus allowing the policy chain to modify the response.. That
   * message will be the result of the next-operation of the policy.
   *
   * If no {@link SourcePolicyParametersTransformer} is provided, then the same response from the flow is going to be routed as
   * response of the next-operation of the policy chain. In this case, the same response from the flow is going to be used to
   * generate the response or failure response for the source so the policy chain is not going to be able to modify the response
   * sent by the source.
   *
   * When the flow execution fails, it will create a {@link FlowExecutionException} instead of a regular
   * {@link MessagingException} to signal that the failure was through the the flow exception and not the policy logic.
   */
  @Override
  protected Publisher<CoreEvent> processNextOperation(CoreEvent event) {
    return just(event)
        .flatMap(request -> from(processWithChildContext(request, flowExecutionProcessor, empty())))
        .map(flowExecutionResponse -> {
          originalResponseParameters =
              getParametersProcessor().getSuccessfulExecutionResponseParametersFunction().apply(flowExecutionResponse);
          Message message = getParametersTransformer()
              .map(parametersTransformer -> parametersTransformer
                  .fromSuccessResponseParametersToMessage(originalResponseParameters))
              .orElseGet(flowExecutionResponse::getMessage);
          return CoreEvent.builder(event).message(message).build();

        })
        .onErrorMap(MessagingException.class, messagingException -> {
          originalFailureResponseParameters =
              getParametersProcessor().getFailedExecutionResponseParametersFunction().apply(messagingException.getEvent());
          Message message = getParametersTransformer()
              .map(parametersTransformer -> parametersTransformer
                  .fromFailureResponseParametersToMessage(originalFailureResponseParameters))
              .orElse(messagingException.getEvent().getMessage());
          MessagingException flowExecutionException =
              new FlowExecutionException(CoreEvent.builder(messagingException.getEvent()).message(message).build(),
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
  protected Publisher<CoreEvent> processPolicy(Policy policy, Processor nextProcessor, CoreEvent event) {
    return just(event)
        .doOnNext(s -> logEvent(getCoreEventId(event), getPolicyName(policy), () -> getCoreEventAttributesAsString(event),
                                "Starting Policy "))
        .transform(sourcePolicyProcessorFactory.createSourcePolicy(policy, nextProcessor))
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
  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent) {
    return from(MessageProcessors.process(sourceEvent, getPolicyProcessor()))
        .<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>map(policiesResultEvent -> {
          Supplier<Map<String, Object>> responseParameters = () -> getParametersTransformer()
              .map(parametersTransformer -> concatMaps(originalResponseParameters, parametersTransformer
                  .fromMessageToSuccessResponseParameters(policiesResultEvent.getMessage())))
              .orElse(originalResponseParameters);
          return right(new SourcePolicySuccessResult(policiesResultEvent, responseParameters, getParametersProcessor()));
        }).doOnNext(result -> logSourcePolicySuccessfullResult(result.getRight()))

        .doOnError(e -> !(e instanceof FlowExecutionException || e instanceof MessagingException),
                   e -> LOGGER.error(e.getMessage(), e))
        .onErrorResume(FlowExecutionException.class, e -> {
          Supplier<Map<String, Object>> responseParameters = () -> getParametersTransformer()
              .map(parametersTransformer -> concatMaps(originalFailureResponseParameters, parametersTransformer
                  .fromMessageToErrorResponseParameters(e.getEvent().getMessage())))
              .orElse(originalFailureResponseParameters);
          return just(left(new SourcePolicyFailureResult(e, responseParameters)));
        })
        .onErrorResume(MessagingException.class, e -> {
          Supplier<Map<String, Object>> responseParameters =
              () -> getParametersTransformer().map(parametersTransformer -> concatMaps(originalFailureResponseParameters,
                                                                                       parametersTransformer
                                                                                           .fromMessageToErrorResponseParameters(e
                                                                                               .getEvent().getMessage())))
                  .orElse(originalFailureResponseParameters);
          return just(Either
              .<SourcePolicyFailureResult, SourcePolicySuccessResult>left(new SourcePolicyFailureResult(e, responseParameters)))
                  .doOnNext(result -> logSourcePolicyFailureResult(result
                      .getLeft()));
        });
  }

  private Map<String, Object> concatMaps(Map<String, Object> originalResponseParameters,
                                         Map<String, Object> policyResponseParameters) {
    Map<String, Object> concatMap = new HashMap<>();
    if (originalResponseParameters != null) {
      concatMap.putAll(originalResponseParameters);
    }
    concatMap.putAll(policyResponseParameters);
    return concatMap;
  }

  private void logEvent(String eventId, String policyName, Supplier<String> message, String startingMessage) {
    if (LOGGER.isTraceEnabled()) {
      //TODO Remove event id when first policy generates it. MULE-14455
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
}
