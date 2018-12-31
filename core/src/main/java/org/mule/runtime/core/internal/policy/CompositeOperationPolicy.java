/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.completeSuccessIfNeeded;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * {@link OperationPolicy} created from a list of {@link Policy}.
 * <p>
 * Implements the template methods from {@link AbstractCompositePolicy} required to work with operation policies.
 *
 * @since 4.0
 */
public class CompositeOperationPolicy
    extends AbstractCompositePolicy<OperationPolicyParametersTransformer, OperationExecutionFunction>
    implements OperationPolicy, Disposable {

  private static final String POLICY_OPERATION_NEXT_OPERATION_RESPONSE = "policy.operation.nextOperationResponse";
  public static final String POLICY_OPERATION_PARAMETERS_PROCESSOR = "policy.operation.parametersProcessor";
  public static final String POLICY_OPERATION_OPERATION_EXEC_FUNCTION = "policy.operation.operationExecutionFunction";
  public static final String POLICY_OPERATION_CALLER_SINK = "policy.operation.callerSink";

  private final OperationPolicyProcessorFactory operationPolicyProcessorFactory;

  private final FluxSink<CoreEvent> policySink;

  /**
   * Creates a new composite policy.
   * <p>
   * If a non-empty {@code operationPolicyParametersTransformer} is passed to this class, then it will be used to convert the flow
   * execution response parameters to a message with the content of such parameters in order to allow the pipeline after the
   * next-operation to modify the response. If an empty {@code operationPolicyParametersTransformer} is provided then the policy
   * won't be able to change the response parameters of the source and the original response parameters generated from the source
   * will be used.
   *
   * @param parameterizedPolicies list of {@link Policy} to chain together.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param operationPolicyProcessorFactory factory for creating each {@link OperationPolicy} from a {@link Policy}
   */
  public CompositeOperationPolicy(List<Policy> parameterizedPolicies,
                                  Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                  OperationPolicyProcessorFactory operationPolicyProcessorFactory) {
    super(parameterizedPolicies, operationPolicyParametersTransformer);
    this.operationPolicyProcessorFactory = operationPolicyProcessorFactory;

    AtomicReference<FluxSink<CoreEvent>> sinkRef = new AtomicReference<>();

    Flux<CoreEvent> policyFlux =
        Flux.<CoreEvent>create(sink -> sinkRef.set(sink))
            .transform(getExecutionProcessor())
            .doOnNext(result -> {
              ((MonoSink<CoreEvent>) ((InternalEvent) result)
                  .getInternalParameter(POLICY_OPERATION_CALLER_SINK)).success(result);
            })
            .doOnError(t -> {
              t.printStackTrace();
            })
            .onErrorContinue(MessagingException.class, (t, e) -> {
              final MessagingException me = (MessagingException) t;

              final MonoSink<CoreEvent> callerSink = (MonoSink<CoreEvent>) ((InternalEvent) me.getEvent())
                  .getInternalParameter(POLICY_OPERATION_CALLER_SINK);
              callerSink.error(t);
            });

    policyFlux.subscribe();
    this.policySink = sinkRef.get();
  }

  /**
   * Stores the operation result so all the chains after the operation execution are executed with the actual operation result and
   * not a modified version from another policy.
   *
   * @param eventPub the event to execute the operation.
   */
  @Override
  protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub) {
    return Flux.from(eventPub)
        .flatMap(event -> {
          Map<String, Object> parametersMap = new HashMap<>();
          OperationParametersProcessor parametersProcessor =
              ((InternalEvent) event).getInternalParameter(POLICY_OPERATION_PARAMETERS_PROCESSOR);
          parametersMap.putAll(parametersProcessor.getOperationParameters());

          if (getParametersTransformer().isPresent()) {
            parametersMap.putAll(getParametersTransformer().get().fromMessageToParameters(event.getMessage()));
          }

          OperationExecutionFunction operationExecutionFunction =
              ((InternalEvent) event).getInternalParameter(POLICY_OPERATION_OPERATION_EXEC_FUNCTION);
          return operationExecutionFunction.execute(parametersMap, event);
        })
        .map(response -> quickCopy(response, singletonMap(POLICY_OPERATION_NEXT_OPERATION_RESPONSE, response)));
  }

  /**
   * Always uses the stored result of {@code processNextOperation} so all the chains after the operation execution are executed
   * with the actual operation result and not a modified version from another policy.
   *
   * @param policy the policy to execute.
   * @param nextProcessor the processor to execute when the policy next-processor gets executed
   * @param eventPub the event to use to execute the policy chain.
   */
  @Override
  protected Publisher<CoreEvent> applyPolicy(Policy policy, ReactiveProcessor nextProcessor, Publisher<CoreEvent> eventPub) {
    ReactiveProcessor defaultOperationPolicy = operationPolicyProcessorFactory.createOperationPolicy(policy, nextProcessor);
    return Flux.from(eventPub)
        .transform(defaultOperationPolicy)
        .map(policyResponse -> {

          if (policy.getPolicyChain().isPropagateMessageTransformations()) {
            return quickCopy(policyResponse, singletonMap(POLICY_OPERATION_NEXT_OPERATION_RESPONSE, policyResponse));
          }

          final InternalEvent nextOperationResponse =
              ((InternalEvent) policyResponse).getInternalParameter(POLICY_OPERATION_NEXT_OPERATION_RESPONSE);
          return nextOperationResponse != null ? nextOperationResponse : policyResponse;
        })
        .cast(CoreEvent.class);
  }

  @Override
  public Publisher<CoreEvent> process(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                                      OperationParametersProcessor parametersProcessor) {
    BaseEventContext childContext = newChildContext(operationEvent, empty());

    return Mono.<CoreEvent>create(callerSink -> {
      internalProcessWithChildContext(operationEvent,
                                      childPub -> Mono.from(childPub)
                                          .doOnNext(childEvent -> policySink
                                              .next(operationEventForPolicy(childEvent, operationExecutionFunction,
                                                                            parametersProcessor, callerSink))),
                                      childContext)
                                          .subscribe();
    })
        .doOnNext(completeSuccessIfNeeded(childContext, true))
        .switchIfEmpty(from(childContext.getResponsePublisher()))
        .map(result -> quickCopy(childContext.getParentContext().get(), result))
        .doOnError(MessagingException.class,
                   me -> me.setProcessedEvent(quickCopy(childContext.getParentContext().get(), me.getEvent())))
        .doOnSuccess(result -> {
          if (result == null) {
            childContext.getParentContext().get().success();
          }
        });
  }

  /**
   * Creates a new {@link BaseEventContext} which is child of the one in the given {@code event}
   *
   * @param event the parent event
   * @param componentLocation the location of the component creating the child context
   * @return a child {@link BaseEventContext}
   */
  public static BaseEventContext newChildContext(CoreEvent event, Optional<ComponentLocation> componentLocation) {
    return child(((BaseEventContext) event.getContext()), componentLocation);
  }

  private static Mono<CoreEvent> internalProcessWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                                 BaseEventContext child) {
    return just(quickCopy(child, event)).transform(processor);
  }

  private CoreEvent operationEventForPolicy(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                                            OperationParametersProcessor parametersProcessor, MonoSink<CoreEvent> callerSink) {
    CoreEvent operationEventForPolicy = quickCopy(operationEvent, of(POLICY_OPERATION_PARAMETERS_PROCESSOR, parametersProcessor,
                                                                     POLICY_OPERATION_OPERATION_EXEC_FUNCTION,
                                                                     operationExecutionFunction,
                                                                     POLICY_OPERATION_CALLER_SINK, callerSink));

    final CoreEvent event = getParametersTransformer().isPresent() ? CoreEvent.builder(operationEventForPolicy)
        .message(getParametersTransformer().get().fromParametersToMessage(parametersProcessor.getOperationParameters()))
        .build() : operationEventForPolicy;
    return event;
  }

  @Override
  public void dispose() {}
}
