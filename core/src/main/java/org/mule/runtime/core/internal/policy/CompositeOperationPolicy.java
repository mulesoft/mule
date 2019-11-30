/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.Runtime.getRuntime;
import static java.util.Optional.of;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.collection.SmallMap.of;
import static org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock.readWriteLock;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.policy.OperationPolicyContext.OPERATION_POLICY_CONTEXT;
import static org.mule.runtime.core.internal.policy.OperationPolicyContext.from;
import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChildContext;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.create;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.RoundRobinFluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.TransactionAwareFluxSinkSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

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

  private final Component operation;
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
   * @param operation                            the operation on which the policies will be applied
   * @param parameterizedPolicies                list of {@link Policy} to chain together.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param operationPolicyProcessorFactory      factory for creating each {@link OperationPolicy} from a {@link Policy}
   */
  public CompositeOperationPolicy(Component operation, List<Policy> parameterizedPolicies,
                                  Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                  OperationPolicyProcessorFactory operationPolicyProcessorFactory) {
    super(parameterizedPolicies, operationPolicyParametersTransformer);
    this.operation = operation;
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

      Flux<CoreEvent> policyFlux = create(sinkRef)
          .transform(getExecutionProcessor())
          .doOnNext(result -> {
            OperationPolicyContext ctx = from((InternalEvent) result);
            final BaseEventContext childContext = ctx.getOperationChildContext();
            if (!childContext.isComplete()) {
              childContext.success(result);
            }
            ctx.getOperationCallerCallback().complete(quickCopy(childContext.getParentContext().get(), result));
          })
          .onErrorContinue(MessagingException.class, (t, e) -> {
            final MessagingException me = (MessagingException) t;
            OperationPolicyContext ctx = from((InternalEvent) me.getEvent());

            final BaseEventContext childContext = ctx.getOperationChildContext();
            if (!childContext.isComplete()) {
              childContext.error(me);
            }
            me.setProcessedEvent(quickCopy(childContext.getParentContext().get(), me.getEvent()));

            ctx.getOperationCallerCallback().error(me);
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
    FluxSinkRecorder<Either<CoreEvent, Throwable>> sinkRecorder = new FluxSinkRecorder<>();
    final Flux<CoreEvent> doOnNext = from(eventPub)
        .doOnNext(event -> {
          OperationPolicyContext ctx = from(event);
          OperationExecutionFunction operationExecutionFunction = ctx.getOperationExecutionFunction();

          operationExecutionFunction.execute(resolveOperationParameters(event, ctx), event, new ExecutorCallback() {

            @Override
            public void complete(Object value) {
              sinkRecorder.next(left((CoreEvent) value, Throwable.class));
            }

            @Override
            public void error(Throwable e) {
              // if `sink.error` is called here, it will cancel the flux altogether. That's why an `Either` is used here, so the
              // error can be propagated afterwards in a way consistent with our expected error handling.
              sinkRecorder.next(right(CoreEvent.class, mapError(e, event)));
            }

            private Throwable mapError(Throwable t, CoreEvent event) {
              t = Exceptions.unwrap(t);
              if (!(t instanceof MessagingException)) {
                t = new MessagingException(event, t, operation);
              }
              return t;
            }
          });
        })
        .doOnComplete(() -> sinkRecorder.complete());

    return subscribeFluxOnPublisherSubscription(create(sinkRecorder)
                                                    .map(result -> {
                                                      result.applyRight(t -> {
                                                        throw propagate(t);
                                                      });
                                                      return result.getLeft();
                                                    }), doOnNext)
        .doOnNext(response -> from(response).setNextOperationResponse((InternalEvent) response));
  }

  private Map<String, Object> resolveOperationParameters(CoreEvent event, OperationPolicyContext ctx) {
    OperationParametersProcessor parametersProcessor = ctx.getOperationParametersProcessor();
    final Map<String, Object> operationParameters = parametersProcessor.getOperationParameters();

    return getParametersTransformer()
        .map(paramsTransformer -> {
          Map<String, Object> parametersMap = new HashMap<>(operationParameters);
          parametersMap.putAll(paramsTransformer.fromMessageToParameters(event.getMessage()));
          return parametersMap;
        })
        .orElse(operationParameters);
  }

  /**
   * Always uses the stored result of {@code processNextOperation} so all the chains after the operation execution are executed
   * with the actual operation result and not a modified version from another policy.
   *
   * @param policy        the policy to execute.
   * @param nextProcessor the processor to execute when the policy next-processor gets executed
   * @param eventPub      the event to use to execute the policy chain.
   */
  @Override
  protected Publisher<CoreEvent> applyPolicy(Policy policy, ReactiveProcessor nextProcessor, Publisher<CoreEvent> eventPub) {
    return from(eventPub)
        .transform(operationPolicyProcessorFactory.createOperationPolicy(policy, nextProcessor));
  }

  @Override
  public void process(CoreEvent operationEvent,
                      OperationExecutionFunction operationExecutionFunction,
                      OperationParametersProcessor parametersProcessor,
                      ComponentLocation operationLocation,
                      ExecutorCallback callback) {

    readWriteLock.withReadLock(() -> {
      if (!disposed.get()) {
        FluxSink<CoreEvent> policySink = policySinks.get(operationLocation.getLocation()).get();

        policySink.next(operationEventForPolicy(quickCopy(newChildContext(operationEvent, of(operationLocation)), operationEvent),
                                                operationExecutionFunction,
                                                parametersProcessor, callback));
      } else {
        callback.error(new MessagingException(createStaticMessage("Operation policy already disposed"), operationEvent));
      }
    });
  }

  private CoreEvent operationEventForPolicy(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                                            OperationParametersProcessor parametersProcessor, ExecutorCallback callback) {
    OperationPolicyContext ctx = new OperationPolicyContext(parametersProcessor,
                                                            operationExecutionFunction,
                                                            (BaseEventContext) operationEvent.getContext(),
                                                            callback);
    if (getParametersTransformer().isPresent()) {
      return InternalEvent.builder(operationEvent)
          .message(getParametersTransformer().get().fromParametersToMessage(parametersProcessor.getOperationParameters()))
          .addInternalParameter(OPERATION_POLICY_CONTEXT, ctx)
          .build();
    } else {
      return quickCopy(operationEvent, of(OPERATION_POLICY_CONTEXT, ctx));
    }
  }

  @Override
  public void dispose() {
    readWriteLock.withWriteLock(() -> {
      policySinks.invalidateAll();
      disposed.set(true);
    });
  }
}
