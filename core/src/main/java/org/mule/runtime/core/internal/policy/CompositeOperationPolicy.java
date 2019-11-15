/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static com.google.common.collect.ImmutableMap.of;
import static java.lang.Runtime.getRuntime;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock.readWriteLock;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChildContext;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.RoundRobinFluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.TransactionAwareFluxSinkSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;

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
    extends AbstractCompositePolicy<OperationPolicyParametersTransformer>
    implements OperationPolicy, Disposable {

  public static final String POLICY_OPERATION_NEXT_OPERATION_RESPONSE = "policy.operation.nextOperationResponse";
  public static final String POLICY_OPERATION_PARAMETERS_PROCESSOR = "policy.operation.parametersProcessor";
  public static final String POLICY_OPERATION_OPERATION_EXEC_FUNCTION = "policy.operation.operationExecutionFunction";
  private static final String POLICY_OPERATION_CHILD_CTX = "policy.operation.childContext";
  private static final String POLICY_OPERATION_CALLER_SINK = "policy.operation.callerSink";

  private final OperationPolicyProcessorFactory operationPolicyProcessorFactory;

  private final LoadingCache<String, FluxSinkSupplier<CoreEvent>> policySinks;

  private final AtomicBoolean disposed;
  private final FunctionalReadWriteLock readWriteLock;

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
    this.disposed = new AtomicBoolean(false);
    this.readWriteLock = readWriteLock();
    this.policySinks = newBuilder()
        .removalListener((String key, FluxSinkSupplier<CoreEvent> value, RemovalCause cause) -> {
          value.dispose();
        })
        .build(componentLocation -> {
          Supplier<FluxSink<CoreEvent>> factory = new OperationWithPoliciesFluxObjectFactory();
          return new TransactionAwareFluxSinkSupplier<>(factory,
                                                        new RoundRobinFluxSinkSupplier<>(getRuntime().availableProcessors(),
                                                                                         factory));
        });
  }

  private final class OperationWithPoliciesFluxObjectFactory implements Supplier<FluxSink<CoreEvent>> {

    @Override
    public FluxSink<CoreEvent> get() {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();

      Flux<CoreEvent> policyFlux =
          Flux.create(sinkRef)
              .transform(getExecutionProcessor())
              .doOnNext(result -> {
                final BaseEventContext childContext = getStoredChildContext(result);
                if (!childContext.isComplete()) {
                  childContext.success(result);
                }
                ((MonoSink<CoreEvent>) ((InternalEvent) result).getInternalParameter(POLICY_OPERATION_CALLER_SINK))
                    .success(quickCopy(childContext.getParentContext().get(), result));
              })
              .onErrorContinue(MessagingException.class, (t, e) -> {
                final MessagingException me = (MessagingException) t;

                final BaseEventContext childContext = getStoredChildContext(me.getEvent());
                if (!childContext.isComplete()) {
                  childContext.error(me);
                }
                me.setProcessedEvent(quickCopy(childContext.getParentContext().get(),
                                               me.getEvent()));
                ((MonoSink<CoreEvent>) ((InternalEvent) me.getEvent()).getInternalParameter(POLICY_OPERATION_CALLER_SINK))
                    .error(me);
              });

      policyFlux.subscribe();
      return sinkRef.getFluxSink();
    }
  }

  /**
   * Stores the operation result so all the chains after the operation execution are executed with the actual operation result and
   * not a modified version from another policy.
   *
   * @param eventPub the event to execute the operation.
   */
  @Override
  protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub, Policy lastPolicy) {
    return Flux.from(eventPub)
        .flatMap(event -> {
          OperationParametersProcessor parametersProcessor =
              ((InternalEvent) event).getInternalParameter(POLICY_OPERATION_PARAMETERS_PROCESSOR);
          Map<String, Object> parametersMap = new HashMap<>(parametersProcessor.getOperationParameters());

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
    return Flux.from(eventPub).transform(defaultOperationPolicy);
  }

  @Override
  public Publisher<CoreEvent> process(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                                      OperationParametersProcessor parametersProcessor, ComponentLocation operationLocation) {
    return readWriteLock.withReadLock(lockReleaser -> {
      if (!disposed.get()) {
        return Mono.create(callerSink -> {
          FluxSink<CoreEvent> policySink = policySinks.get(operationLocation.getLocation()).get();
          policySink
              .next(operationEventForPolicy(quickCopy(newChildContext(operationEvent, of(operationLocation)), operationEvent),
                                            operationExecutionFunction,
                                            parametersProcessor, callerSink));
        });
      } else {
        MessagingException me = new MessagingException(createStaticMessage("Operation policy already disposed"), operationEvent);
        return Mono.error(me);
      }
    });
  }

  private CoreEvent operationEventForPolicy(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                                            OperationParametersProcessor parametersProcessor, MonoSink<CoreEvent> callerSink) {
    return getParametersTransformer().isPresent()
        ? InternalEvent.builder(operationEvent)
            .message(getParametersTransformer().get().fromParametersToMessage(parametersProcessor.getOperationParameters()))
            .addInternalParameter(POLICY_OPERATION_PARAMETERS_PROCESSOR, parametersProcessor)
            .addInternalParameter(POLICY_OPERATION_OPERATION_EXEC_FUNCTION, operationExecutionFunction)
            .addInternalParameter(POLICY_OPERATION_CHILD_CTX, operationEvent.getContext())
            .addInternalParameter(POLICY_OPERATION_CALLER_SINK, callerSink)
            .build()
        : quickCopy(operationEvent, of(POLICY_OPERATION_PARAMETERS_PROCESSOR, parametersProcessor,
                                       POLICY_OPERATION_OPERATION_EXEC_FUNCTION, operationExecutionFunction,
                                       POLICY_OPERATION_CHILD_CTX, operationEvent.getContext(),
                                       POLICY_OPERATION_CALLER_SINK, callerSink));
  }

  private static BaseEventContext getStoredChildContext(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_OPERATION_CHILD_CTX);
  }

  @Override
  public void dispose() {
    readWriteLock.withWriteLock(() -> {
      policySinks.invalidateAll();
      disposed.set(true);
    });
  }
}
