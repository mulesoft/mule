/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.Runtime.getRuntime;
import static org.mule.runtime.api.config.MuleRuntimeFeature.HONOUR_ERROR_MAPPINGS_WHEN_POLICY_APPLIED_ON_OPERATION;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.util.collection.SmallMap.copy;
import static org.mule.runtime.core.internal.policy.OperationPolicyContext.from;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.RoundRobinFluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.TransactionAwareFluxSinkSupplier;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
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
    implements OperationPolicy, Disposable, DeferredDisposable {

  private static final Logger LOGGER = getLogger(CompositeOperationPolicy.class);

  private final Component operation;
  private final OperationPolicyProcessorFactory operationPolicyProcessorFactory;

  private final LoadingCache<String, FluxSinkSupplier<CoreEvent>> policySinks;

  private final long shutdownTimeout;
  private final Scheduler completionCallbackScheduler;
  private final PolicyTraceLogger policyTraceLogger = new PolicyTraceLogger();

  private FeatureFlaggingService featureFlaggingService;

  /**
   * Creates a new composite policy.
   * <p>
   * If a non-empty {@code operationPolicyParametersTransformer} is passed to this class, then it will be used to convert the flow
   * execution response parameters to a message with the content of such parameters in order to allow the pipeline after the
   * next-operation to modify the response. If an empty {@code operationPolicyParametersTransformer} is provided then the policy
   * won't be able to change the response parameters of the source and the original response parameters generated from the source
   * will be used.
   *
   * @param operation the operation on which the policies will be applied
   * @param parameterizedPolicies list of {@link Policy} to chain together.
   * @param operationPolicyParametersTransformer transformer from the operation parameters to a message and vice versa.
   * @param operationPolicyProcessorFactory factory for creating each {@link OperationPolicy} from a {@link Policy}.
   * @param completionCallbackScheduler the executor where the completion of the policy flux will happen.
   * @param shutdownTimeout how long to wait for pending items to finish processing before actually completing the flux for the
   *        policy.
   */
  public CompositeOperationPolicy(Component operation, List<Policy> parameterizedPolicies,
                                  Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer,
                                  OperationPolicyProcessorFactory operationPolicyProcessorFactory,
                                  long shutdownTimeout, Scheduler completionCallbackScheduler,
                                  FeatureFlaggingService featureFlaggingService) {
    super(parameterizedPolicies, operationPolicyParametersTransformer);
    this.operation = operation;
    this.operationPolicyProcessorFactory = operationPolicyProcessorFactory;
    this.shutdownTimeout = shutdownTimeout;
    this.completionCallbackScheduler = completionCallbackScheduler;
    this.featureFlaggingService = featureFlaggingService;
    initProcessor();

    Supplier<FluxSink<CoreEvent>> factory = new OperationWithPoliciesFluxObjectFactory(this, featureFlaggingService);
    this.policySinks = newBuilder()
        .removalListener((String key, FluxSinkSupplier<CoreEvent> value, RemovalCause cause) -> {
          value.dispose();
        })
        .build(componentLocation -> {
          return new TransactionAwareFluxSinkSupplier<>(factory,
                                                        new RoundRobinFluxSinkSupplier<>(getRuntime().availableProcessors(),
                                                                                         factory));
        });
  }

  private static final class OperationWithPoliciesFluxObjectFactory implements Supplier<FluxSink<CoreEvent>> {

    private final Reference<CompositeOperationPolicy> compositeOperationPolicy;
    private FeatureFlaggingService featureFlaggingService;

    public OperationWithPoliciesFluxObjectFactory(CompositeOperationPolicy compositeOperationPolicy,
                                                  FeatureFlaggingService featureFlaggingService) {
      this.compositeOperationPolicy = new WeakReference<>(compositeOperationPolicy);
      this.featureFlaggingService = featureFlaggingService;
    }

    @Override
    public FluxSink<CoreEvent> get() {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();

      Flux<CoreEvent> policyFlux = sinkRef.flux()
          .transform(compositeOperationPolicy.get().getExecutionProcessor())
          .doOnNext(result -> from(result).getOperationCallerCallback().complete(result))
          .onErrorContinue(MessagingException.class, (t, e) -> {
            final MessagingException me = (MessagingException) t;
            if (featureFlaggingService.isEnabled(HONOUR_ERROR_MAPPINGS_WHEN_POLICY_APPLIED_ON_OPERATION)) {
              // Nullyfing error so that the error is resolved again by the operation's execution logic and its error mappings
              // (W-11147961)
              final CoreEvent event = CoreEvent.builder(me.getEvent()).error(null)
                  .build();
              me.setProcessedEvent(event);
            }
            from(me.getEvent()).getOperationCallerCallback().error(me);
          });

      policyFlux.subscribe(null, e -> LOGGER.error("Exception reached subscriber for " + toString(), e));

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
  protected Publisher<CoreEvent> applyNextOperation(Publisher<CoreEvent> eventPub) {
    FluxSinkRecorder<Either<Throwable, CoreEvent>> sinkRecorder = new FluxSinkRecorder<>();

    return from(propagateCompletion(from(eventPub), sinkRecorder.flux(), pub -> from(pub)
        .doOnNext(new OperationDispatcher(sinkRecorder, getParametersTransformer(), this.operation))
        .map(e -> Either.empty()), sinkRecorder::complete, sinkRecorder::error,
                                    shutdownTimeout, completionCallbackScheduler,
                                    operation.getDslSource()))
                                        .map(result -> {
                                          result.applyLeft(t -> {
                                            throw propagate(t);
                                          });
                                          return result.getRight();
                                        }).doOnNext(response -> from(response)
                                            .setNextOperationResponse((InternalEvent) response));
  }

  private static final class OperationDispatcher implements Consumer<CoreEvent> {

    private final FluxSinkRecorder<Either<Throwable, CoreEvent>> sinkRecorder;
    private final Optional<OperationPolicyParametersTransformer> parametersTransformer;
    private final Component operation;

    public OperationDispatcher(FluxSinkRecorder<Either<Throwable, CoreEvent>> sinkRecorder,
                               Optional<OperationPolicyParametersTransformer> parametersTransformer, Component operation) {
      this.sinkRecorder = sinkRecorder;
      this.parametersTransformer = parametersTransformer;
      this.operation = operation;
    }

    @Override
    public void accept(CoreEvent event) {
      OperationPolicyContext ctx = from(event);
      OperationExecutionFunction operationExecutionFunction = ctx.getOperationExecutionFunction();

      operationExecutionFunction.execute(resolveOperationParameters(event, parametersTransformer, ctx), event,
                                         new ExecutorCallback() {

                                           @Override
                                           public void complete(Object value) {
                                             sinkRecorder.next(right(Throwable.class, (CoreEvent) value));
                                           }

                                           @Override
                                           public void error(Throwable e) {
                                             // if `sink.error` is called here, it will cancel the flux altogether. That's why an
                                             // `Either` is used here, so
                                             // the error can be propagated afterwards in a way consistent with our expected error
                                             // handling.
                                             sinkRecorder.next(left(mapError(e, event), CoreEvent.class));
                                           }

                                           private Throwable mapError(Throwable t, CoreEvent event) {
                                             t = Exceptions.unwrap(t);
                                             if (!(t instanceof MessagingException)) {
                                               t = new MessagingException(event, t, operation);
                                             }
                                             return t;
                                           }
                                         });
    }
  }

  private static Map<String, Object> resolveOperationParameters(CoreEvent event,
                                                                Optional<OperationPolicyParametersTransformer> parametersTransformer,
                                                                OperationPolicyContext ctx) {
    OperationParametersProcessor parametersProcessor = ctx.getOperationParametersProcessor();
    final Map<String, Object> operationParameters = parametersProcessor.getOperationParameters();

    return parametersTransformer
        .map(paramsTransformer -> {
          Map<String, Object> parametersMap = copy(operationParameters);
          parametersMap.putAll(paramsTransformer.fromMessageToParameters(event.getMessage()));
          return parametersMap;
        })
        .orElse(operationParameters);
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
    return from(eventPub).transform(operationPolicyProcessorFactory.createOperationPolicy(policy, nextProcessor));
  }

  @Override
  public void process(CoreEvent operationEvent,
                      OperationExecutionFunction operationExecutionFunction,
                      OperationParametersProcessor parametersProcessor,
                      ComponentLocation operationLocation,
                      ExecutorCallback callback) {

    FluxSink<CoreEvent> policySink = policySinks.get(operationLocation.getLocation()).get();

    policySink.next(operationEventForPolicy(operationEvent,
                                            operationExecutionFunction,
                                            parametersProcessor, callback));
  }

  private CoreEvent operationEventForPolicy(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                                            OperationParametersProcessor parametersProcessor, ExecutorCallback callback) {
    OperationPolicyContext ctx = new OperationPolicyContext(parametersProcessor,
                                                            operationExecutionFunction,
                                                            callback);
    if (getParametersTransformer().isPresent()) {
      operationEvent = InternalEvent.builder(operationEvent)
          .message(getParametersTransformer().get().fromParametersToMessage(parametersProcessor.getOperationParameters()))
          .build();
    }
    ((InternalEvent) operationEvent).setOperationPolicyContext(ctx);
    return operationEvent;
  }

  @Override
  public void dispose() {
    policySinks.invalidateAll();
    completionCallbackScheduler.stop();
  }

  @Override
  public Disposable deferredDispose() {
    final LoadingCache<String, FluxSinkSupplier<CoreEvent>> policySinks = this.policySinks;
    final Scheduler completionCallbackScheduler = this.completionCallbackScheduler;
    return () -> {
      policySinks.invalidateAll();
      completionCallbackScheduler.stop();
    };
  }
}
