/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.config.MuleRuntimeFeature.SUPPRESS_ERRORS;
import static org.mule.runtime.api.el.ExpressionLanguageUtils.sanitize;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_THREAD_RELEASE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.isSanitizedPayload;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_COMPONENT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.internal.policy.DefaultPolicyManager.noPolicyOperation;
import static org.mule.runtime.core.internal.policy.PolicyManager.NOOP_POLICY_MANAGER;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.PROCESSOR_SCHEDULER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.runtime.core.internal.util.rx.RxUtils.createRoundRobinFluxSupplier;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateErrorResponseMapper;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_TO_APPLY;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.createDefaultProcessingStrategyFactory;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.getLocalOperatorErrorHook;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.isNoTransactionalAction;
import static org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory.extractExecutorInitialisationParams;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;
import static org.mule.runtime.module.extension.internal.util.InterceptorChainUtils.createConnectionInterceptorsChain;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toActionCode;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.GET_CONNECTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.OPERATION_EXECUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.PARAMETERS_RESOLUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.VALUE_RESOLUTION_SPAN_NAME;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.deferContextual;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.internal.interception.ParametersResolverProcessor;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.processor.strategy.ComponentInnerProcessor;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.transaction.MuleTransactionConfig;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.RxUtils;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.transaction.TransactionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.LazyExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.OperationExecutionParams;
import org.mule.runtime.module.extension.internal.runtime.operation.adapter.SdkOperationTransactionalActionUtils;
import org.mule.runtime.module.extension.internal.runtime.operation.retry.ComponentRetryPolicyTemplateResolver;
import org.mule.runtime.module.extension.internal.runtime.operation.retry.RetryPolicyTemplateResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RouteBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.result.PayloadTargetReturnDelegate;
import org.mule.runtime.module.extension.internal.runtime.result.ReturnDelegate;
import org.mule.runtime.module.extension.internal.runtime.result.TargetReturnDelegate;
import org.mule.runtime.module.extension.internal.runtime.result.ValueReturnDelegate;
import org.mule.runtime.module.extension.internal.runtime.result.VoidReturnDelegate;
import org.mule.runtime.module.extension.internal.runtime.tracing.TracedResolverSet;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;
import org.mule.sdk.api.tx.OperationTransactionalAction;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.MDC;

import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

/**
 * A {@link Processor} capable of executing extension components.
 * <p>
 * If required, it obtains a configuration instance, evaluate all the operation parameters and executes it by using a
 * {@link #componentExecutor}. This message processor is capable of serving the execution of any {@link } of any
 * {@link ExtensionModel}.
 * <p>
 * A {@link #componentExecutor} is obtained by testing the {@link T} for a {@link CompletableComponentExecutorModelProperty}
 * through which a {@link CompletableComponentExecutorFactory} is obtained. Models with no such property cannot be used with this
 * class. The obtained {@link CompletableComponentExecutor} serve all invocations of {@link #process(CoreEvent)} on {@code this}
 * instance but will not be shared with other instances of {@link ComponentMessageProcessor}. All the {@link Lifecycle} events
 * that {@code this} instance receives will be propagated to the {@link #componentExecutor}.
 * <p>
 * The {@link #componentExecutor} is executed directly but by the means of a {@link DefaultExecutionMediator}
 * <p>
 * Before executing the operation will use the {@link PolicyManager} to lookup for a {@link OperationPolicy} that must be applied
 * to the operation. If there's a policy to be applied then it will interleave the operation execution with the policy logic
 * allowing the policy to execute logic over the operation parameters, change those parameters and then execute logic with the
 * operation response.
 *
 * @since 4.0
 */
public abstract class ComponentMessageProcessor<T extends ComponentModel> extends ExtensionComponent<T>
    implements Processor, ParametersResolverProcessor<T>, Lifecycle {

  public static final String PROCESSOR_PATH_MDC_KEY = "processorPath";
  static final String INVALID_TARGET_MESSAGE =
      "Root component '%s' defines an invalid usage of operation '%s' which uses %s as %s";
  private static final Logger LOGGER = getLogger(ComponentMessageProcessor.class);
  private static final ExtensionTransactionFactory TRANSACTION_FACTORY = new ExtensionTransactionFactory();
  protected final ExtensionModel extensionModel;
  protected ResolverSet resolverSet;
  protected final String target;
  protected final String targetValue;
  protected final RetryPolicyTemplate retryPolicyTemplate;
  protected final MessageProcessorChain nestedChain;

  private final ReflectionCache reflectionCache;
  private final ResultTransformer resultTransformer;
  private final boolean hasNestedChain;
  private final long outerFluxTerminationTimeout;
  private final Object fluxSupplierLock = new Object();

  private final AtomicInteger activeOuterPublishersCount = new AtomicInteger(0);

  protected ExecutionMediator executionMediator;
  protected CompletableComponentExecutor componentExecutor;
  protected ReturnDelegate returnDelegate;

  protected ClassLoader nestedChainClassLoader;
  private Optional<TransactionConfig> transactionConfig;

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  private ExtensionConnectionSupplier extensionConnectionSupplier;

  @Inject
  private InternalProfilingService profilingService;

  @Inject
  private ComponentTracerFactory<CoreEvent> componentTracerFactory;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  private MuleConfiguration muleConfiguration;

  @Inject
  private NotificationDispatcher notificationDispatcher;

  @Inject
  private ArtifactEncoding artifactEncoding;

  @Inject
  private Optional<PolicyManager> policyManager;

  private Function<Optional<ConfigurationInstance>, RetryPolicyTemplate> retryPolicyResolver;
  private String resolvedProcessorRepresentation;
  private boolean initialised = false;

  private ProcessingStrategy processingStrategy;
  private boolean ownedProcessingStrategy = false;
  private volatile FluxSinkSupplier<CoreEvent> fluxSupplier;

  private Scheduler outerFluxCompletionScheduler;

  /*
   * TODO: MULE-18483 When a policy is applied to an operation that has defined a target, it's necessary to wait until the policy
   * finishes to calculate the return value with {@link #returnDelegate}. But in this case, because of in order to execute the
   * rest of the policy we need to transform the {@link Result} returned by the operation into a {@link CoreEvent}, we use {@link
   * #valueReturnDelegate} as a helper class to do this transformation. It's used only when there is an operation that defines a
   * target, and at the same time, there are operation policies applied to it. Finally, when the policy finishes, the proper
   * {@link #returnDelegate} is executed. It'd be ideal to improve this by extracting from {@link ReturnDelegate} the logic that
   * transforms an {@link Object} into a {@link CoreEvent}.
   */
  private ReturnDelegate valueReturnDelegate;
  private String processorPath = null;
  private ComponentTracer<CoreEvent> operationParametersResolutionTracer =
      DummyComponentTracerFactory.DUMMY_COMPONENT_TRACER_INSTANCE;

  public ComponentMessageProcessor(ExtensionModel extensionModel,
                                   T componentModel,
                                   ValueResolver<ConfigurationProvider> configurationProviderResolver,
                                   String target,
                                   String targetValue,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   MessageProcessorChain nestedChain,
                                   ClassLoader classLoader,
                                   ExtensionManager extensionManager,
                                   ReflectionCache reflectionCache,
                                   ResultTransformer resultTransformer,
                                   long terminationTimeout) {
    super(extensionModel, componentModel, configurationProviderResolver, cursorProviderFactory, extensionManager);
    this.extensionModel = extensionModel;
    this.resolverSet = resolverSet;
    this.target = target;
    this.targetValue = targetValue;
    this.retryPolicyTemplate = retryPolicyTemplate;
    this.nestedChain = nestedChain;
    this.nestedChainClassLoader = classLoader;
    this.reflectionCache = reflectionCache;
    this.resultTransformer = resultTransformer;
    this.hasNestedChain = hasNestedChain(componentModel);
    this.outerFluxTerminationTimeout = terminationTimeout;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    final BiFunction<Throwable, Object, Throwable> localOperatorErrorHook =
        getLocalOperatorErrorHook(this, errorTypeLocator, exceptionContextProviders);
    final boolean mayCompleteInDifferentThread = mayCompleteInDifferentThread();
    final ComponentLocation location = getLocation();

    return deferContextual(ctx -> {
      Flux<CoreEvent> transformed =
          createOuterFlux(from(publisher), localOperatorErrorHook, mayCompleteInDifferentThread, ctx)
              .doOnNext(result -> removeSdkInternalContextFromResult(location, result))
              .map(propagateErrorResponseMapper());

      if (publisher instanceof Flux && !ctx.getOrEmpty(WITHIN_PROCESS_TO_APPLY).isPresent()) {
        return transformed
            .doAfterTerminate(this::outerPublisherTerminated)
            .doOnSubscribe(s -> outerPublisherSubscribedTo());
      } else {
        // Certain features (ext client, batch, flow runner, interception-api) use Mono, so we don't want to dispose the inner
        // stuff after the first event comes through
        return transformed;
      }
    });
  }

  private void removeSdkInternalContextFromResult(final ComponentLocation location, Either<Throwable, CoreEvent> result) {
    result.apply(me -> removeSdkInternalContext(location, ((MessagingException) me).getEvent()),
                 response -> removeSdkInternalContext(location, response));
  }

  private void removeSdkInternalContext(final ComponentLocation location, final CoreEvent event) {
    final SdkInternalContext sdkCtx = from(event);
    if (sdkCtx != null) {
      sdkCtx.removeContext(location, event.getContext().getId());
    }
  }

  private Flux<Either<Throwable, CoreEvent>> createOuterFlux(final Flux<CoreEvent> publisher,
                                                             final BiFunction<Throwable, Object, Throwable> localOperatorErrorHook,
                                                             final boolean mayCompleteInDifferentThread,
                                                             ContextView ctx) {
    final FluxSinkRecorder<Either<Throwable, CoreEvent>> errorSwitchSinkSinkRef = new FluxSinkRecorder<>();

    final Function<Publisher<CoreEvent>, Publisher<Either<Throwable, CoreEvent>>> transformer =
        pub -> from(pub)
            .mapNotNull(event -> {
              try {
                return addContextToEvent(event, ctx);
              } catch (Exception t) {
                // Force the error mapper from the chain to be used.
                // When using Mono.create with sink.error, the error mapper from the
                // context is ignored, so it has to be explicitly used here.
                final Throwable mapped = localOperatorErrorHook.apply(t, event);

                if (outerFluxTerminationTimeout < 0
                    // When there is a mono involved in some part of the chain, we cannot use the termination timeout because that
                    // would
                    // impose a timeout in the operation, which we don't want to.
                    // In this case, the flux will be complete when there are no more inflight operations.
                    || ctx.getOrDefault(WITHIN_PROCESS_TO_APPLY, false)) {
                  // if `sink.error` is called here, it will cancel the flux altogether.
                  // That's why an `Either` is used here,
                  // so the error can be propagated afterwards in a way consistent with our expected error handling.
                  errorSwitchSinkSinkRef.next(left(mapped, CoreEvent.class));
                  return null;
                }
                throw propagateWrappingFatal(mapped);
              }
            })
            .doOnNext(event -> {

              final ExecutorCallback executorCallback = new ExecutorCallback() {

                @Override
                public void error(Throwable e) {
                  // if `sink.error` is called here, it will cancel the flux altogether.
                  // That's why an `Either` is used here,
                  // so the error can be propagated afterwards in a way consistent with our expected error handling.
                  errorSwitchSinkSinkRef.next(left(
                                                   // Force the error mapper from the chain to be used.
                                                   // When using Mono.create with sink.error, the error mapper from the
                                                   // context is ignored, so it has to be explicitly used here.
                                                   localOperatorErrorHook.apply(e, event), CoreEvent.class));
                }

                @Override
                public void complete(Object value) {
                  errorSwitchSinkSinkRef.next(right(Throwable.class, (CoreEvent) value));
                }
              };

              if (!mayCompleteInDifferentThread && from(event).isNoPolicyOperation(getLocation(), event.getContext().getId())) {
                onEventSynchronous(event, executorCallback, ctx);
              } else {
                onEvent(event, executorCallback, ctx);
              }
            })
            .map(e -> Either.empty());

    if (outerFluxTerminationTimeout < 0
        // When there is a mono involved in some part of the chain, we cannot use the termination timeout because that would
        // impose a timeout in the operation, which we don't want to.
        // In this case, the flux will be complete when there are no more inflight operations.
        || ctx.getOrDefault(WITHIN_PROCESS_TO_APPLY, false)) {
      return from(propagateCompletion(from(publisher), errorSwitchSinkSinkRef.flux(), transformer,
                                      () -> errorSwitchSinkSinkRef.complete(),
                                      t -> errorSwitchSinkSinkRef.error(t)));
    } else {
      // For fluxes, the only way they would complete is when the flow that owns the flux is stopped.
      // In that case we need to enforce the timeout configured in the app so that the stop of the flow doesn't take more than
      // that time.
      return from(propagateCompletion(from(publisher), errorSwitchSinkSinkRef.flux(), transformer,
                                      () -> errorSwitchSinkSinkRef.complete(),
                                      t -> errorSwitchSinkSinkRef.error(t),
                                      outerFluxTerminationTimeout,
                                      outerFluxCompletionScheduler,
                                      getDslSource()));
    }
  }

  @Override
  public ProcessingType getProcessingType() {
    if (mayCompleteInDifferentThread()) {
      // In this case, any thread switch will be done in the innerFlux
      return CPU_LITE;
    } else {
      // The innerFlux will not be used in this case, so the outer PS needs to be aware of the actual ProcessingType
      return getInnerProcessingType();
    }
  }

  private void onEvent(CoreEvent event, ExecutorCallback executorCallback, ContextView ctx) {
    try {
      SdkInternalContext sdkInternalContext = from(event);
      final ComponentLocation location = getLocation();
      final String eventId = event.getContext().getId();

      final Optional<ConfigurationInstance> configuration = sdkInternalContext.getConfiguration(location, eventId);
      final Map<String, Object> resolutionResult = sdkInternalContext.getResolutionResult(location, eventId);

      OperationExecutionFunction operationExecutionFunction = (parameters, operationEvent, callback) -> {
        setOperationExecutionParams(location, event, configuration, parameters, operationEvent, callback, ctx);
        fluxSupplier.get(ctx).next(operationEvent);
      };

      if (location != null) {
        ((DefaultFlowCallStack) event.getFlowCallStack())
            .pushCurrentProcessorPath(resolvedProcessorRepresentation);

        ExecutorCallback effectiveCallback =
            isTargetWithPolicies(event) ? getExecutionCallbackForPolicyAndOperationWithTarget(event, executorCallback)
                : executorCallback;

        sdkInternalContext.getPolicyToApply(location, eventId).process(event, operationExecutionFunction, () -> resolutionResult,
                                                                       location, effectiveCallback);
      } else {
        // If this operation has no component location then it is internal. Don't apply policies on internal operations.
        operationExecutionFunction.execute(resolutionResult, event, executorCallback);
      }
    } catch (Throwable t) {
      executorCallback.error(unwrap(t));
    }
  }

  private void onEventSynchronous(CoreEvent event, ExecutorCallback executorCallback, ContextView ctx) {
    try {
      SdkInternalContext sdkInternalContext = from(event);
      final ComponentLocation location = getLocation();
      final String eventId = event.getContext().getId();

      final Optional<ConfigurationInstance> configuration = sdkInternalContext.getConfiguration(location, eventId);
      final Map<String, Object> resolutionResult = sdkInternalContext.getResolutionResult(location, eventId);

      OperationExecutionFunction operationExecutionFunction = (parameters, operationEvent, callback) -> {
        setOperationExecutionParams(location, event, configuration, parameters, operationEvent, callback, ctx);

        prepareAndExecuteOperation(event, () -> callback);
      };

      ExecutorCallback effectiveCallback =
          isTargetWithPolicies(event) ? getExecutionCallbackForPolicyAndOperationWithTarget(event, executorCallback)
              : executorCallback;

      operationExecutionFunction.execute(resolutionResult, event, effectiveCallback);
    } catch (Throwable t) {
      executorCallback.error(unwrap(t));
    }
  }

  /**
   * Only used in case the operation defines a target and there are operation policies applied to it.
   *
   * @param event
   * @param delegateCallback
   * @return
   * @see {@link #valueReturnDelegate}
   */
  private ExecutorCallback getExecutionCallbackForPolicyAndOperationWithTarget(CoreEvent event,
                                                                               ExecutorCallback delegateCallback) {
    return new ExecutorCallback() {

      @Override
      public void complete(Object o) {
        ExecutionContextAdapter operationContext = null;
        try {
          OperationExecutionParams operationExecutionParams =
              getOperationExecutionParams(event);

          if (operationExecutionParams != null) {
            operationContext = operationExecutionParams.executionContextAdapter();
            operationContext.changeEvent(event);
          } else {
            // There was an error propagated before <execute-next> and the operation execution parameters don't exist yet.
            operationContext = createExecutionContext(event);
          }

          delegateCallback.complete(returnDelegate.asReturnValue(o, operationContext));
        } catch (MuleException e) {
          delegateCallback.error(e);
        } catch (Throwable t) {
          delegateCallback.error(unwrap(t));
        }
      }

      @Override
      public void error(Throwable t) {
        delegateCallback.error(unwrap(t));
      }
    };
  }

  private ExecutorCallback mapped(ExecutorCallback callback, ExecutionContextAdapter<T> operationContext,
                                  ReturnDelegate delegate) {
    return new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        callback.complete(delegate.asReturnValue(value, operationContext));
      }

      @Override
      public void error(Throwable t) {
        callback.error(unwrap(t));
      }
    };
  }

  // TODO MULE-18482: Decouple policies and operation logic
  private boolean isTargetWithPolicies(CoreEvent event) {
    return !from(event).isNoPolicyOperation(getLocation(), event.getContext().getId()) && !isBlank(target);
  }

  private Optional<ConfigurationInstance> resolveConfiguration(CoreEvent event) {
    if (shouldUsePrecalculatedContext(event)) {
      // Intercepted components optimization: If the event already contains an execution context, use that one.
      // (the context is already calculated as part of the interception)
      return getPrecalculatedContext(event).getConfiguration();
    } else {
      // Otherwise, generate the context as usual.
      return getConfiguration(event);
    }
  }

  private boolean shouldUsePrecalculatedContext(CoreEvent event) {
    final ComponentLocation location = getLocation();
    return location != null && isInterceptedComponent(location, (InternalEvent) event)
        && getPrecalculatedContext(event) != null;
  }

  private PrecalculatedExecutionContextAdapter<T> getPrecalculatedContext(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(INTERCEPTION_RESOLVED_CONTEXT);
  }

  protected void executeOperation(ExecutionContextAdapter<T> operationContext, ExecutorCallback callback) {
    executionMediator.execute(componentExecutor, operationContext, callback);
  }

  private ExecutionContextAdapter<T> createExecutionContext(Optional<ConfigurationInstance> configuration,
                                                            Map<String, Object> resolvedParameters,
                                                            CoreEvent event, Scheduler currentScheduler) {

    return new DefaultExecutionContext<>(extensionModel, configuration, resolvedParameters, componentModel, event,
                                         artifactEncoding,
                                         muleContext.getNotificationManager(),
                                         getCursorProviderFactory(), streamingManager, this,
                                         retryPolicyResolver.apply(configuration), currentScheduler, transactionConfig,
                                         muleContext.getSecurityManager(),
                                         muleContext);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    if (!initialised) {
      initRetryPolicyResolver();
      try {
        transactionConfig = buildTransactionConfig();
      } catch (MuleException e) {
        throw new InitialisationException(createStaticMessage("Could not resolve transactional configuration"), e, this);
      }
      returnDelegate = createReturnDelegate();
      valueReturnDelegate = getValueReturnDelegate();
      initialiseIfNeeded(resolverSet, muleContext);
      componentExecutor = createComponentExecutor();
      executionMediator = createExecutionMediator();
      initialiseIfNeeded(componentExecutor, true, muleContext);

      ComponentLocation componentLocation = getLocation();
      if (componentLocation != null) {
        processorPath = componentLocation.getLocation();
      }

      resolvedProcessorRepresentation = getRepresentation();

      if (nestedChain != null) {
        LOGGER.debug("Initializing nested chain ({}) of component '{}'...", nestedChain, processorPath);
        initialiseIfNeeded(nestedChain, muleContext);
      }

      initProcessingStrategy();

      // Since the tracing feature is Component aware at all levels, we cannot do this wrapping earlier (for example, at component
      // building time)
      operationParametersResolutionTracer = componentTracerFactory.fromComponent(this, PARAMETERS_RESOLUTION_SPAN_NAME, "");
      resolverSet = new TracedResolverSet(muleContext,
                                          componentTracerFactory.fromComponent(this, VALUE_RESOLUTION_SPAN_NAME, ""))
                                              .addAll(resolverSet.getResolvers());
      initialised = true;
    }
  }

  private void initProcessingStrategy() throws InitialisationException {
    final Optional<ProcessingStrategy> processingStrategyFromRootContainer = getProcessingStrategy(componentLocator, this);

    processingStrategy = processingStrategyFromRootContainer
        .orElseGet(() -> createDefaultProcessingStrategyFactory().create(muleContext, toString() + ".ps"));

    if (processingStrategyFromRootContainer.isPresent()) {
      LOGGER.debug("Using processing strategy ({}) from container for component '{}'", processingStrategy, processorPath);
    } else {
      LOGGER.debug("Initializing own processing strategy ({}) of component '{}'...", processingStrategy, processorPath);
      ownedProcessingStrategy = true;
      initialiseIfNeeded(processingStrategy, muleContext);
    }
  }

  private void startInnerFlux() {
    if (fluxSupplier == null) {
      synchronized (fluxSupplierLock) {
        startInnerFluxUnsafe();
      }
    }
  }

  private void startInnerFluxUnsafe() {
    if (fluxSupplier != null) {
      LOGGER.debug("Skipping creation of inner flux supplier for processor '{}' because it is already created.",
                   this.getLocation());
      return;
    }

    // Create and register an internal flux, which will be the one to really use the processing strategy for this operation.
    // This is a round-robin, so it can handle concurrent events and its lifecycle is tied to the lifecycle of the main flux.
    fluxSupplier = createRoundRobinFluxSupplier(p -> {
      final ComponentInnerProcessor innerProcessor = new ComponentInnerProcessor() {

        @Override
        public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
          return deferContextual(ctx -> {
            final FluxSinkRecorder<Either<EventProcessingException, CoreEvent>> emitter = new FluxSinkRecorder<>();

            return from(propagateCompletion(from(publisher), emitter.flux(),
                                            pub -> from(pub)
                                                .doOnNext(innerEventDispatcher(emitter))
                                                .map(e -> Either.empty()),
                                            () -> emitter.complete(), e -> emitter.error(e)))
                                                .map(RxUtils.<EventProcessingException>propagateErrorResponseMapper());
          });
        }

        private Consumer<? super CoreEvent> innerEventDispatcher(final FluxSinkRecorder<Either<EventProcessingException, CoreEvent>> emitter) {
          return event -> prepareAndExecuteOperation(event,
                                                     // The callback must be listened to within the
                                                     // processingStrategy's onProcessor, so that any thread
                                                     // switch that may occur after the operation (for
                                                     // instance, getting away from the selector thread after
                                                     // a non-blocking operation) is actually performed.
                                                     () -> new ExecutorCallback() {

                                                       @Override
                                                       public void complete(Object value) {
                                                         emitter.next(right((CoreEvent) value));
                                                       }

                                                       @Override
                                                       public void error(Throwable e) {
                                                         // if `sink.error` is called here, it will cancel
                                                         // the flux altogether.
                                                         // That's why an `Either` is used here,
                                                         // so the error can be propagated afterwards in a
                                                         // way consistent with our expected error handling.
                                                         emitter.next(left(new EventProcessingException(event, e, false)));
                                                       }
                                                     });
        }

        @Override
        public ProcessingType getProcessingType() {
          return getInnerProcessingType();
        }

        @Override
        public boolean isBlocking() {
          return ComponentMessageProcessor.this.isBlocking();
        }

        @Override
        public ComponentLocation resolveLocation() {
          return ComponentMessageProcessor.this.getLocation();
        }
      };

      return from(processingStrategy
          .configureInternalPublisher(from(p)
              .transform(processingStrategy.onProcessor(innerProcessor))
              .doOnNext(result -> getOperationExecutionParams(result)
                  .callback().complete(result))
              .onErrorContinue((t, result) -> getOperationExecutionParams(((EventProcessingException) t).getEvent())
                  .callback().error(t.getCause()))));
    },
                                                getRuntime().availableProcessors());
  }

  protected OperationExecutionParams getOperationExecutionParams(final CoreEvent event) {
    try {
      return from(event)
          .getOperationExecutionParams(getLocation(), event.getContext().getId());
    } catch (NullPointerException npe) {
      LOGGER.debug("Null SDK Context at {} for event - {}", getLocation() != null ? getLocation().getLocation() : "null",
                   event.getContext().getId());
      throw propagateWrappingFatal(new EventProcessingException(createStaticMessage("Maybe the non-blocking operation @ '"
          + getLocation().getLocation() + "' used its callback more than once?"),
                                                                event, npe));
    }
  }

  private CoreEvent addContextToEvent(CoreEvent event, ContextView ctx) throws MuleException {
    SdkInternalContext sdkInternalContext = from(event);
    if (sdkInternalContext == null) {
      sdkInternalContext = new SdkInternalContext();
      ((InternalEvent) event).setSdkInternalContext(sdkInternalContext);
    }

    final ComponentLocation location = getLocation();
    final String eventId = event.getContext().getId();

    sdkInternalContext.putContext(location, eventId);

    if (hasNestedChain
        && (ctx.hasKey(POLICY_NEXT_OPERATION) || ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS))) {
      sdkInternalContext.setInnerChainSubscriberContextMapping(innerChainCtx -> {
        if (ctx.hasKey(POLICY_NEXT_OPERATION)) {
          innerChainCtx = innerChainCtx.put(POLICY_NEXT_OPERATION, ctx.get(POLICY_NEXT_OPERATION));
        }
        if (ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS)) {
          innerChainCtx = innerChainCtx.put(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS,
                                            ctx.get(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS));
        }
        return innerChainCtx;
      });
    }

    sdkInternalContext.setConfiguration(location, eventId, resolveConfiguration(event));

    final Map<String, Object> resolutionResult;
    if (shouldUsePrecalculatedContext(event)) {
      resolutionResult = getPrecalculatedContext(event).getParameters();
    } else {
      resolutionResult = getResolutionResult(event, sdkInternalContext.getConfiguration(location, eventId));
    }

    sdkInternalContext.setResolutionResult(location, eventId, resolutionResult);
    sdkInternalContext.setPolicyToApply(location, eventId, location != null
        ? policyManager.orElse(NOOP_POLICY_MANAGER).createOperationPolicy(this, event, () -> resolutionResult)
        : noPolicyOperation());

    return event;
  }

  private void setOperationExecutionParams(ComponentLocation location, CoreEvent event,
                                           Optional<ConfigurationInstance> configuration, Map<String, Object> parameters,
                                           CoreEvent operationEvent, ExecutorCallback callback, ContextView ctx) {

    SdkInternalContext sdkInternalContext = SdkInternalContext.from(event);

    final Scheduler currentScheduler = (Scheduler) ctx.getOrEmpty(PROCESSOR_SCHEDULER_CONTEXT_KEY)
        .orElse(IMMEDIATE_SCHEDULER);

    ExecutionContextAdapter<T> operationContext;
    if (shouldUsePrecalculatedContext(operationEvent)) {
      operationContext = getPrecalculatedContext(operationEvent);
      operationContext.setCurrentScheduler(currentScheduler);
      operationContext.changeEvent(event);
    } else {
      operationContext = createExecutionContext(configuration,
                                                parameters,
                                                operationEvent,
                                                currentScheduler);
    }

    sdkInternalContext.setOperationExecutionParams(location, event.getContext().getId(), configuration, parameters,
                                                   operationEvent, callback, operationContext);

  }


  private void prepareAndExecuteOperation(CoreEvent event, Supplier<ExecutorCallback> callbackSupplier) {
    OperationExecutionParams oep = getOperationExecutionParams(event);

    ExecutionContextAdapter<T> operationContext = oep.executionContextAdapter();

    setCurrentEvent((PrivilegedEvent) event);
    boolean wasProcessorPathSet = setCurrentLocation();
    try {
      executeOperation(operationContext, mapped(callbackSupplier.get(), operationContext,
                                                isTargetWithPolicies(event) ? valueReturnDelegate : returnDelegate));
    } finally {
      unsetCurrentLocation(wasProcessorPathSet);
    }
  }

  private boolean setCurrentLocation() {
    if (MDC.get(PROCESSOR_PATH_MDC_KEY) != null) {
      return false;
    }

    if (processorPath == null) {
      return false;
    }

    MDC.put(PROCESSOR_PATH_MDC_KEY, processorPath);
    return true;
  }

  private void unsetCurrentLocation(boolean wasProcessorPathSet) {
    if (wasProcessorPathSet) {
      MDC.remove(PROCESSOR_PATH_MDC_KEY);
    }
  }

  private void initRetryPolicyResolver() {
    Optional<ConfigurationInstance> staticConfig = getStaticConfiguration();
    RetryPolicyTemplateResolver policyResolver = getRetryPolicyTemplateResolver();
    if (staticConfig.isPresent() || !requiresConfig()) {
      RetryPolicyTemplate staticPolicy = policyResolver.fetchRetryPolicyTemplate(staticConfig);
      retryPolicyResolver = config -> staticPolicy;
    } else {
      retryPolicyResolver = policyResolver::fetchRetryPolicyTemplate;
    }
  }

  protected RetryPolicyTemplateResolver getRetryPolicyTemplateResolver() {
    return new ComponentRetryPolicyTemplateResolver(retryPolicyTemplate, connectionManager);
  }

  private CompletableComponentExecutor<T> createComponentExecutor() throws InitialisationException {
    Map<String, Object> params = extractExecutorInitialisationParams(
                                                                     extensionModel,
                                                                     componentModel,
                                                                     resolverSet.getResolvers(),
                                                                     this,
                                                                     getStaticConfiguration(),
                                                                     extensionManager,
                                                                     expressionManager,
                                                                     reflectionCache);

    return getOperationExecutorFactory(componentModel).createExecutor(componentModel, params);
  }

  protected ReturnDelegate createReturnDelegate() {
    if (isVoid(componentModel)) {
      return VoidReturnDelegate.INSTANCE;
    }

    return !isTargetPresent()
        ? getValueReturnDelegate()
        : getTargetReturnDelegate();
  }


  protected ReturnDelegate getTargetReturnDelegate() {
    if (isSanitizedPayload(sanitize(targetValue))) {
      return new PayloadTargetReturnDelegate(target, componentModel, artifactEncoding);
    }
    return new TargetReturnDelegate(target, targetValue, componentModel, expressionManager, artifactEncoding,
                                    streamingManager);
  }

  protected ValueReturnDelegate getValueReturnDelegate() {
    return new ValueReturnDelegate(componentModel, artifactEncoding);
  }

  protected boolean isTargetPresent() {
    if (isBlank(target)) {
      return false;
    }

    if (expressionManager.isExpression(target)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 componentModel.getName(),
                                                 "an expression", TARGET_PARAMETER_NAME));
    } else if (!expressionManager.isExpression(targetValue)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 componentModel.getName(), "something that is not an expression",
                                                 TARGET_VALUE_PARAMETER_NAME));
    }

    return true;
  }


  protected boolean isBlocking() {
    return !mayCompleteInDifferentThread();
  }

  /**
   * This indicates that the component message processor may jump threads under certain conditions (not necessarily always). For
   * example, it may jump threads only when a connection problem happens and the retry strategy is triggered.
   *
   * @return whether it may jump threads.
   */
  protected boolean mayCompleteInDifferentThread() {
    if (!requiresConfig()) {
      return false;
    }

    if (usesDynamicConfiguration()) {
      return true;
    } else {
      Optional<ConfigurationInstance> staticConfig = getStaticConfiguration();
      if (staticConfig.isPresent()) {
        return isAsyncExecutableBasedOn(staticConfig);
      }
    }

    return true;
  }

  protected boolean isAsyncExecutableBasedOn(Optional<ConfigurationInstance> staticConfig) {
    return retryPolicyResolver.apply(staticConfig).isEnabled();
  }

  /**
   * If the {@link #nestedChain} is not null, it changes the current ClassLoader for the Application's ClassLoader
   * {@link #nestedChainClassLoader} to start the processors, and then it goes back to the previous ClassLoader.
   *
   * @throws MuleException if the {@link LifecycleUtils#startIfNeeded} fails.
   */
  protected void startIfNeededNestedChain() throws MuleException {
    if (nestedChain != null) {
      // The Application's ClassLoader (nestedChainClassLoader) is needed to have the proper classpath
      // when a processor needs to load an Application's resource.
      final Thread currentThread = Thread.currentThread();
      final ClassLoader currentClassLoader = currentThread.getContextClassLoader();
      setContextClassLoader(currentThread, currentClassLoader, this.nestedChainClassLoader);
      try {
        LOGGER.debug("Starting nested chain ({}) of component '{}'...", nestedChain, processorPath);
        startIfNeeded(nestedChain);
      } finally {
        setContextClassLoader(currentThread, this.nestedChainClassLoader, currentClassLoader);
      }
    }
  }

  @Override
  public void doStart() throws MuleException {
    startIfNeeded(componentExecutor);

    startIfNeededNestedChain();

    startIfNeeded(getRoutes());

    if (ownedProcessingStrategy) {
      LOGGER.debug("Starting own processing strategy ({}) of component '{}'...", processingStrategy, processorPath);
      startIfNeeded(processingStrategy);
    }
    if (outerFluxTerminationTimeout >= 0) {
      outerFluxCompletionScheduler = muleContext.getSchedulerService().ioScheduler(muleContext.getSchedulerBaseConfig()
          .withMaxConcurrentTasks(1).withName(toString() + ".outer.flux."));
      LOGGER.debug("Created outerFluxCompletionScheduler ({}) of component '{}'", outerFluxCompletionScheduler, processorPath);
    }

    LOGGER.debug("Starting inner flux of component '{}'...", processorPath);
    startInnerFlux();
  }

  @Override
  public void doStop() throws MuleException {
    if (nestedChain != null) {
      LOGGER.debug("Sttopping nested chain ({}) of component '{}'...", nestedChain, processorPath);
      stopIfNeeded(nestedChain);
    }

    stopIfNeeded(getRoutes());

    stopIfNeeded(componentExecutor);
    LOGGER.debug("Stopping inner flux of component '{}'...", processorPath);
    stopInnerFlux();

    if (ownedProcessingStrategy) {
      LOGGER.debug("Stopping own processing strategy ({}) of component '{}'...", processingStrategy, processorPath);
      stopIfNeeded(processingStrategy);
    }

    if (outerFluxTerminationTimeout >= 0 && outerFluxCompletionScheduler != null) {
      LOGGER.debug("Stopping outerFluxCompletionScheduler ({}) of component '{}'...", outerFluxCompletionScheduler,
                   processorPath);
      outerFluxCompletionScheduler.stop();
      outerFluxCompletionScheduler = null;
    }
  }

  private Collection<RouteBuilderValueResolver> getRoutes() {
    return resolverSet.getResolvers().values().stream().filter(resolver -> resolver instanceof RouteBuilderValueResolver)
        .map(resolver -> (RouteBuilderValueResolver) resolver).collect(toList());
  }

  private void outerPublisherSubscribedTo() {
    // We don't want a potentially concurrent publisher termination to stop the fluxes after we start them
    // that is why an AtomicInteger is not enough, we need to hold the lock of the counter while we restart the inner flux
    // that way we ensure the inner flux is available if and only if there are active publishers...
    // ...unless ComponentMessageProcessor#stop is called, which is a different story
    synchronized (activeOuterPublishersCount) {
      // This doesn't need to be an AtomicInteger as long as it happens inside a synchronized block, but it is preferred for
      // readability.
      if (activeOuterPublishersCount.getAndIncrement() == 0) {
        startInnerFlux();
      }
    }
  }

  private void outerPublisherTerminated() {
    // See the comment in #outerPublisherSubscribedTo
    synchronized (activeOuterPublishersCount) {
      if (activeOuterPublishersCount.decrementAndGet() == 0) {
        stopInnerFlux();
      }
    }
  }

  private void stopInnerFlux() {
    if (fluxSupplier != null) {
      synchronized (fluxSupplierLock) {
        if (fluxSupplier != null) {
          fluxSupplier.dispose();
          fluxSupplier = null;
        }
      }
    }
  }

  @Override
  public void doDispose() {
    if (nestedChain != null) {
      LOGGER.debug("Disposing nested chain ({}) of component '{}'...", nestedChain, processorPath);
      disposeIfNeeded(nestedChain, LOGGER);
    }

    disposeIfNeeded(getRoutes(), LOGGER);

    disposeIfNeeded(componentExecutor, LOGGER);
    if (ownedProcessingStrategy) {
      LOGGER.debug("Disposing own processing strategy ({}) of component '{}'...", ownedProcessingStrategy, processorPath);
      disposeIfNeeded(processingStrategy, LOGGER);
    }
    initialised = false;
  }

  protected ExecutionMediator createExecutionMediator() {
    return new DefaultExecutionMediator(extensionModel,
                                        componentModel,
                                        createConnectionInterceptorsChain(extensionModel, componentModel,
                                                                          extensionConnectionSupplier, reflectionCache,
                                                                          componentTracerFactory
                                                                              .fromComponent(this, GET_CONNECTION_SPAN_NAME, "")),
                                        errorTypeRepository,
                                        muleContext.getExecutionClassLoader(),
                                        muleConfiguration,
                                        ArtifactType.valueOf(muleContext.getArtifactType().name()),
                                        notificationDispatcher,
                                        resultTransformer,
                                        profilingService.getProfilingDataProducer(OPERATION_THREAD_RELEASE),
                                        componentTracerFactory.fromComponent(this, OPERATION_EXECUTION_SPAN_NAME, ""),
                                        featureFlaggingService.isEnabled(SUPPRESS_ERRORS));
  }

  /**
   * Validates that the {@link #componentModel} is valid for the given {@code configurationProvider}
   *
   * @throws IllegalOperationException If the validation fails
   */
  @Override
  protected abstract void validateOperationConfiguration(ConfigurationProvider configurationProvider);

  /**
   * This is the processing type that is actually taken into account when the processing strategy is applied. This is used by the
   * flux created in {@link #startInnerFlux()}.
   */
  public ProcessingType getInnerProcessingType() {
    return CPU_LITE;
  }

  @Override
  public void resolveParameters(CoreEvent.Builder eventBuilder,
                                BiConsumer<Map<String, Supplier<Object>>, ExecutionContext> afterConfigurer)
      throws MuleException {
    ExecutionContextAdapter<T> delegateExecutionContext = createExecutionContext(eventBuilder.build());
    PrecalculatedExecutionContextAdapter executionContext = new PrecalculatedExecutionContextAdapter(delegateExecutionContext);

    final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;
    Throwable throwable = mediator.applyBeforeInterceptors(executionContext);
    if (throwable == null) {
      final Map<String, Supplier<Object>> resolvedArguments = getArgumentResolver()
          .apply(executionContext);
      afterConfigurer.accept(resolvedArguments, executionContext);
      executionContext.changeEvent(eventBuilder.build());
    } else {
      throw new DefaultMuleException("Interception execution for operation not ok", throwable);
    }
  }

  private Function<ExecutionContext<T>, Map<String, Object>> getArgumentResolver() {
    if (componentExecutor instanceof OperationArgumentResolverFactory) {
      return ((OperationArgumentResolverFactory<T>) componentExecutor)
          .createArgumentResolver(componentModel);
    }

    return ec -> Collections.emptyMap();
  }

  @Override
  public void disposeResolvedParameters(ExecutionContext<T> executionContext) {
    ((DefaultExecutionMediator) executionMediator).applyAfterInterceptors(executionContext);
  }

  private ExecutionContextAdapter<T> createExecutionContext(CoreEvent event) throws MuleException {
    Optional<ConfigurationInstance> configuration = getConfiguration(event);
    return createExecutionContext(configuration, getResolutionResult(event, configuration), event, IMMEDIATE_SCHEDULER);
  }

  private Map<String, Object> getResolutionResult(CoreEvent event, Optional<ConfigurationInstance> configuration)
      throws MuleException {
    operationParametersResolutionTracer.startSpan(event);
    try (ValueResolvingContext context = ValueResolvingContext.builder(event, expressionManager)
        .withConfig(configuration)
        .withLocation(getLocation()).build()) {
      return resolverSet.resolve(context).asMap();
    } finally {
      operationParametersResolutionTracer.endCurrentSpan(event);
    }
  }

  private boolean isInterceptedComponent(ComponentLocation location, InternalEvent event) {
    final Component component = event.getInternalParameter(INTERCEPTION_COMPONENT);
    if (component != null) {
      return location.equals(component.getLocation());
    }
    return false;
  }

  private boolean supportsTransactions(T componentModel) {
    return componentModel instanceof ConnectableComponentModel && ((ConnectableComponentModel) componentModel).isTransactional();
  }

  private boolean requiresTransactionalActionConfiguration(T componentModel) {
    return !isNoTransactionalAction(componentModel);
  }

  private boolean hasNestedChain(T componentModel) {
    return componentModel.getNestedComponents().stream()
        .anyMatch(nestedComp -> nestedComp instanceof NestedRouteModel
            || ((NestedComponentModel) nestedComp).getAllowedStereotypes().stream().anyMatch(st -> st.isAssignableTo(PROCESSOR)));
  }

  private Optional<TransactionConfig> buildTransactionConfig() throws MuleException {
    if (!supportsTransactions(componentModel) || !requiresTransactionalActionConfiguration(componentModel)) {
      return empty();
    }
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig();
    transactionConfig.setAction(toActionCode(getTransactionalAction()));
    transactionConfig.setMuleContext(muleContext);
    transactionConfig.setFactory(TRANSACTION_FACTORY);

    return of(transactionConfig);
  }

  private OperationTransactionalAction getTransactionalAction() throws MuleException {
    ValueResolver<Object> resolver =
        (ValueResolver<Object>) resolverSet.getResolvers().get(TRANSACTIONAL_ACTION_PARAMETER_NAME);
    if (resolver == null) {
      throw new IllegalArgumentException(
                                         format("Operation '%s' from extension '%s' is transactional but no transactional action defined",
                                                componentModel.getName(),
                                                extensionModel.getName()));
    }

    CoreEvent initializerEvent = getNullEvent();
    try {
      return SdkOperationTransactionalActionUtils.from(resolver.resolve(ValueResolvingContext.builder(initializerEvent).build()));
    } finally {
      ((BaseEventContext) initializerEvent.getContext()).success();
    }
  }

  @Override
  public String toString() {
    final ComponentLocation location = getLocation();
    return location != null ? location.getLocation() : super.toString();
  }

  @Inject
  public void setMuleConfiguration(MuleConfiguration muleConfiguration) {
    this.muleConfiguration = muleConfiguration;
  }

  /////////////////////////////////////////////////////////////////////////////
  // "Fat" Tooling support
  /////////////////////////////////////////////////////////////////////////////

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    CoreEvent event = getNullEvent();
    try (ValueResolvingContext ctx = ValueResolvingContext.builder(event, expressionManager).build()) {
      LazyExecutionContext<?> executionContext = new LazyExecutionContext<>(resolverSet, componentModel, extensionModel, ctx);
      return new OperationParameterValueResolver<>(executionContext, resolverSet, reflectionCache, expressionManager);
    } finally {
      ((BaseEventContext) event.getContext()).success();
    }
  }

}
