/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.ComponentLocationProvider.resolveProcessorRepresentation;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.isSanitizedPayload;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.sanitize;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_COMPONENT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.internal.policy.DefaultPolicyManager.noPolicyOperation;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.PROCESSOR_SCHEDULER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.runtime.core.internal.util.rx.RxUtils.createRoundRobinFluxSupplier;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_TO_APPLY;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.createDefaultProcessingStrategyFactory;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.getLocalOperatorErrorHook;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.create;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.OperationExecutionFunction;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.processor.ParametersResolverProcessor;
import org.mule.runtime.core.internal.processor.strategy.OperationInnerProcessor;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldOperationParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.LazyExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionInterceptor;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationArgumentResolverFactory;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.OperationExecutionParams;
import org.mule.runtime.module.extension.internal.runtime.execution.interceptor.InterceptorChain;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConfigOverrideValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.streaming.CursorResetInterceptor;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.util.context.Context;

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

  private static final Logger LOGGER = getLogger(ComponentMessageProcessor.class);

  static final String INVALID_TARGET_MESSAGE =
      "Root component '%s' defines an invalid usage of operation '%s' which uses %s as %s";

  private final ReflectionCache reflectionCache;
  private final RetryPolicyTemplate fallbackRetryPolicyTemplate = new NoRetryPolicyTemplate();

  protected final ExtensionModel extensionModel;
  private final boolean hasNestedChain;
  protected final ResolverSet resolverSet;
  protected final String target;
  protected final String targetValue;
  protected final RetryPolicyTemplate retryPolicyTemplate;

  private final Object fluxSupplierDisposeLock = new Object();

  private final AtomicInteger activeOuterPublishersCount = new AtomicInteger(0);

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  private ExtensionConnectionSupplier extensionConnectionSupplier;

  private Function<Optional<ConfigurationInstance>, RetryPolicyTemplate> retryPolicyResolver;
  private String resolvedProcessorRepresentation;
  private boolean initialised = false;

  private ProcessingStrategy processingStrategy;
  private boolean ownedProcessingStrategy = false;
  private FluxSinkSupplier<CoreEvent> fluxSupplier;

  private Scheduler outerFluxCompletionScheduler;

  protected ExecutionMediator executionMediator;
  protected CompletableComponentExecutor componentExecutor;
  protected ReturnDelegate returnDelegate;
  protected PolicyManager policyManager;

  public ComponentMessageProcessor(ExtensionModel extensionModel,
                                   T componentModel,
                                   ConfigurationProvider configurationProvider,
                                   String target,
                                   String targetValue,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   ExtensionManager extensionManager,
                                   PolicyManager policyManager,
                                   ReflectionCache reflectionCache) {
    super(extensionModel, componentModel, configurationProvider, cursorProviderFactory, extensionManager);
    this.extensionModel = extensionModel;
    this.resolverSet = resolverSet;
    this.target = target;
    this.targetValue = targetValue;
    this.policyManager = policyManager;
    this.retryPolicyTemplate = retryPolicyTemplate;
    this.reflectionCache = reflectionCache;

    this.hasNestedChain = componentModel.getNestedComponents().stream()
        .anyMatch(nestedComp -> {
          return nestedComp instanceof NestedRouteModel
              || ((NestedComponentModel) nestedComp).getAllowedStereotypes().stream()
                  .anyMatch(st -> st.isAssignableTo(PROCESSOR));
        });
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    final BiFunction<Throwable, Object, Throwable> localOperatorErrorHook =
        getLocalOperatorErrorHook(this, errorTypeLocator, exceptionContextProviders);
    final boolean async = isAsync();

    return subscriberContext()
        .flatMapMany(ctx -> {
          final FluxSinkRecorder<Either<Throwable, CoreEvent>> errorSwitchSinkSinkRef = new FluxSinkRecorder<>();

          Flux<CoreEvent> transformed = from(propagateCompletion(from(publisher), create(errorSwitchSinkSinkRef)
              .doOnNext(result -> {
                result.apply(me -> {
                  final SdkInternalContext sdkCtx = from(((MessagingException) me).getEvent());
                  if (sdkCtx != null) {
                    sdkCtx.popContext();
                  }
                }, response -> {
                  final SdkInternalContext sdkCtx = from(response);
                  if (sdkCtx != null) {
                    sdkCtx.popContext();
                  }
                });
              })
              .map(result -> {
                return result.reduce(me -> {
                  throw propagateWrappingFatal(me);
                }, response -> response);
              }), pub -> from(pub)
                  .map(event -> {
                    try {
                      return addContextToEvent(event, ctx);
                    } catch (MuleException t) {
                      throw propagateWrappingFatal(localOperatorErrorHook.apply(t, event));
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

                    if (!async && from(event).isNoPolicyOperation()) {
                      onEventSynchronous(event, executorCallback, ctx);
                    } else {
                      onEvent(event, executorCallback);
                    }
                  }), () -> errorSwitchSinkSinkRef.complete(), t -> errorSwitchSinkSinkRef.error(t),
                                                                 muleContext.getConfiguration().getShutdownTimeout(),
                                                                 outerFluxCompletionScheduler));

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

  @Override
  public ProcessingType getProcessingType() {
    if (isAsync()) {
      // In this case, any thread switch will be done in the innerFlux
      return CPU_LITE;
    } else {
      // The innerFlux will not be used in this case, so the outer PS needs to be aware of the actual ProcessingType
      return getInnerProcessingType();
    }
  }

  private void onEvent(CoreEvent event, ExecutorCallback executorCallback) {
    try {
      SdkInternalContext sdkInternalContext = from(event);

      final Optional<ConfigurationInstance> configuration = sdkInternalContext.getConfiguration();
      final Map<String, Object> resolutionResult = sdkInternalContext.getResolutionResult();

      OperationExecutionFunction operationExecutionFunction = (parameters, operationEvent, callback) -> {
        sdkInternalContext.setOperationExecutionParams(configuration, parameters, operationEvent, callback);

        fluxSupplier.get().next(operationEvent);
      };

      if (getLocation() != null) {
        ((DefaultFlowCallStack) event.getFlowCallStack())
            .setCurrentProcessorPath(resolvedProcessorRepresentation);
        sdkInternalContext.getPolicyToApply().process(event, operationExecutionFunction, () -> resolutionResult, getLocation(),
                                                      executorCallback);
      } else {
        // If this operation has no component location then it is internal. Don't apply policies on internal operations.
        operationExecutionFunction.execute(resolutionResult, event, executorCallback);
      }
    } catch (Throwable t) {
      executorCallback.error(unwrap(t));
    }
  }

  private void onEventSynchronous(CoreEvent event, ExecutorCallback executorCallback, Context ctx) {
    try {
      SdkInternalContext sdkInternalContext = from(event);

      final Optional<ConfigurationInstance> configuration = sdkInternalContext.getConfiguration();
      final Map<String, Object> resolutionResult = sdkInternalContext.getResolutionResult();

      OperationExecutionFunction operationExecutionFunction = (parameters, operationEvent, callback) -> {
        sdkInternalContext.setOperationExecutionParams(configuration, parameters, operationEvent, callback);

        prepareAndExecuteOperation(event, () -> callback, ctx);
      };

      operationExecutionFunction.execute(resolutionResult, event, executorCallback);
    } catch (Throwable t) {
      executorCallback.error(unwrap(t));
    }
  }

  private ExecutorCallback mapped(ExecutorCallback callback, ExecutionContextAdapter<T> operationContext) {
    return new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        callback.complete(returnDelegate.asReturnValue(value, operationContext));
      }

      @Override
      public void error(Throwable t) {
        callback.error(unwrap(t));
      }
    };
  }

  private Optional<ConfigurationInstance> resolveConfiguration(CoreEvent event) {
    if (shouldUsePrecalculatedContext(event)) {
      // If the event already contains an execution context, use that one.
      // Only for interceptable components!
      return getPrecalculatedContext(event).getConfiguration();
    } else {
      // Otherwise, generate the context as usual.
      return getConfiguration(event);
    }
  }

  private boolean shouldUsePrecalculatedContext(CoreEvent event) {
    return getLocation() != null && isInterceptedComponent(getLocation(), (InternalEvent) event)
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
                                         getCursorProviderFactory(), streamingManager, this,
                                         getRetryPolicyTemplate(configuration), currentScheduler, muleContext);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    if (!initialised) {
      initRetryPolicyResolver();
      returnDelegate = createReturnDelegate();
      initialiseIfNeeded(resolverSet, muleContext);
      componentExecutor = createComponentExecutor();
      executionMediator = createExecutionMediator();
      initialiseIfNeeded(componentExecutor, true, muleContext);

      resolvedProcessorRepresentation =
          resolveProcessorRepresentation(muleContext.getConfiguration().getId(), toString(), this);

      initProcessingStrategy();
      initialised = true;
    }
  }

  private void initProcessingStrategy() throws InitialisationException {
    final Optional<ProcessingStrategy> processingStrategyFromRootContainer =
        getProcessingStrategy(componentLocator, getRootContainerLocation());

    processingStrategy = processingStrategyFromRootContainer
        .orElseGet(() -> createDefaultProcessingStrategyFactory().create(muleContext, toString() + ".ps"));

    if (!processingStrategyFromRootContainer.isPresent()) {
      ownedProcessingStrategy = true;
      initialiseIfNeeded(processingStrategy);
    }
  }

  private void startInnerFlux() {
    // Create and register an internal flux, which will be the one to really use the processing strategy for this operation.
    // This is a round robin so it can handle concurrent events, and its lifecycle is tied to the lifecycle of the main flux.
    fluxSupplier = createRoundRobinFluxSupplier(p -> {
      return from(processingStrategy
          .configureInternalPublisher(from(p)
              .transform(processingStrategy.onProcessor(new OperationInnerProcessor() {

                @Override
                public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
                  return subscriberContext()
                      .flatMapMany(ctx -> from(publisher)
                          .doOnNext(event -> prepareAndExecuteOperation(event,
                                                                        () -> from(event)
                                                                            .getOperationExecutionParams()
                                                                            .getCallback(),
                                                                        ctx)));
                }

                @Override
                public ProcessingType getProcessingType() {
                  return getInnerProcessingType();
                }

                @Override
                public boolean isAsync() {
                  return ComponentMessageProcessor.this.isAsync();
                }
              }))
              .onErrorContinue((t, event) -> LOGGER.error("Unhandled error in operation (" + toString() + ") flux",
                                                          t))));
    },
                                                getRuntime().availableProcessors());
  }

  private CoreEvent addContextToEvent(CoreEvent event, Context ctx) throws MuleException {
    SdkInternalContext sdkInternalContext = from(event);
    if (sdkInternalContext == null) {
      sdkInternalContext = new SdkInternalContext();
      ((InternalEvent) event).setSdkInternalContext(sdkInternalContext);
    }
    sdkInternalContext.pushContext();

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

    sdkInternalContext.setConfiguration(resolveConfiguration(event));
    final Map<String, Object> resolutionResult = getResolutionResult(event, sdkInternalContext.getConfiguration());
    sdkInternalContext.setResolutionResult(resolutionResult);
    sdkInternalContext.setPolicyToApply(getLocation() != null
        ? policyManager.createOperationPolicy(this, event, () -> resolutionResult)
        : noPolicyOperation());

    return event;
  }

  private void prepareAndExecuteOperation(CoreEvent event, Supplier<ExecutorCallback> callbackSupplier, Context ctx) {
    OperationExecutionParams oep = from(event).getOperationExecutionParams();

    final Scheduler currentScheduler = (Scheduler) ctx.getOrEmpty(PROCESSOR_SCHEDULER_CONTEXT_KEY)
        .orElse(IMMEDIATE_SCHEDULER);

    ExecutionContextAdapter<T> operationContext;
    if (shouldUsePrecalculatedContext(event)) {
      operationContext = getPrecalculatedContext(oep.getOperationEvent());
      operationContext.setCurrentScheduler(currentScheduler);
    } else {
      operationContext = createExecutionContext(oep.getConfiguration(),
                                                oep.getParameters(),
                                                oep.getOperationEvent(),
                                                currentScheduler);
    }

    executeOperation(operationContext, mapped(callbackSupplier.get(), operationContext));
  }

  private void initRetryPolicyResolver() {
    Optional<ConfigurationInstance> staticConfig = getStaticConfiguration();
    if (staticConfig.isPresent() || !requiresConfig()) {
      RetryPolicyTemplate staticPolicy = fetchRetryPolicyTemplate(staticConfig);
      retryPolicyResolver = config -> staticPolicy;
    } else {
      retryPolicyResolver = this::fetchRetryPolicyTemplate;
    }
  }

  private RetryPolicyTemplate getRetryPolicyTemplate(Optional<ConfigurationInstance> configuration) {
    return retryPolicyResolver.apply(configuration);
  }

  private RetryPolicyTemplate fetchRetryPolicyTemplate(Optional<ConfigurationInstance> configuration) {
    RetryPolicyTemplate delegate = null;
    if (retryPolicyTemplate != null) {
      delegate = configuration
          .map(config -> config.getConnectionProvider().orElse(null))
          .map(provider -> connectionManager.getReconnectionConfigFor(provider).getRetryPolicyTemplate(retryPolicyTemplate))
          .orElse(retryPolicyTemplate);
    }

    // In case of no template available in the context, use the one defined by the ConnectionProvider
    if (delegate == null) {
      delegate = configuration
          .map(config -> config.getConnectionProvider().orElse(null))
          .map(provider -> connectionManager.getRetryTemplateFor((ConnectionProvider<? extends Object>) provider))
          .orElse(fallbackRetryPolicyTemplate);
    }

    return delegate;
  }

  private CompletableComponentExecutor<T> createComponentExecutor() throws InitialisationException {
    Map<String, Object> params = new HashMap<>();

    LazyValue<ValueResolvingContext> resolvingContext =
        new LazyValue<>(() -> {
          CoreEvent initialiserEvent = null;
          try {
            initialiserEvent = getNullEvent();
            return ValueResolvingContext.builder(initialiserEvent, expressionManager)
                .withConfig(getStaticConfiguration())
                .build();
          } finally {
            if (initialiserEvent != null) {
              ((BaseEventContext) initialiserEvent.getContext()).success();
            }
          }
        });

    LazyValue<Boolean> dynamicConfig = new LazyValue<>(
                                                       () -> extensionManager
                                                           .getConfigurationProvider(extensionModel, componentModel,
                                                                                     resolvingContext.get().getEvent())
                                                           .map(ConfigurationProvider::isDynamic)
                                                           .orElse(false));

    try {
      for (ParameterGroupModel group : componentModel.getParameterGroupModels()) {
        if (group.getName().equals(DEFAULT_GROUP_NAME)) {
          for (ParameterModel p : group.getParameterModels()) {
            if (!p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent()) {
              continue;
            }

            ValueResolver<?> resolver = resolverSet.getResolvers().get(p.getName());
            if (resolver != null) {
              params.put(getMemberName(p), resolveComponentExecutorParam(resolvingContext, dynamicConfig, p, resolver));
            }
          }
        } else {
          ParameterGroupDescriptor groupDescriptor = group.getModelProperty(ParameterGroupModelProperty.class)
              .map(g -> g.getDescriptor())
              .orElse(null);

          if (groupDescriptor == null) {
            continue;
          }

          List<ParameterModel> fieldParameters = getGroupsOfFieldParameters(group);

          if (fieldParameters.isEmpty()) {
            continue;
          }

          ObjectBuilder groupBuilder = createFieldParameterGroupBuilder(groupDescriptor, fieldParameters);

          try {
            params.put(((Field) groupDescriptor.getContainer()).getName(), groupBuilder.build(resolvingContext.get()));
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        }
      }

      return getOperationExecutorFactory(componentModel).createExecutor(componentModel, params);
    } finally {
      resolvingContext.ifComputed(ValueResolvingContext::close);
    }
  }

  private Object resolveComponentExecutorParam(LazyValue<ValueResolvingContext> resolvingContext,
                                               LazyValue<Boolean> dynamicConfig,
                                               ParameterModel p,
                                               ValueResolver<?> resolver)
      throws InitialisationException {
    Object resolvedValue;
    try {
      if (resolver instanceof ConfigOverrideValueResolverWrapper) {
        resolvedValue = ((ConfigOverrideValueResolverWrapper<?>) resolver).resolveWithoutConfig(resolvingContext.get());
        if (resolvedValue == null) {
          if (dynamicConfig.get()) {
            String message = format(
                                    "Component '%s' at %s uses a dynamic configuration and defines configuration override parameter '%s' which "
                                        + "is assigned on initialization. That combination is not supported. Please use a non dynamic configuration "
                                        + "or don't set the parameter.",
                                    getLocation() != null ? getLocation().getComponentIdentifier().getIdentifier().toString()
                                        : toString(),
                                    toString(),
                                    p.getName());
            throw new InitialisationException(createStaticMessage(message), this);
          } else {
            resolvedValue = resolver.resolve(resolvingContext.get());
          }
        }
      } else {
        resolvedValue = resolveValue(resolver, resolvingContext.get());
      }

      return resolvedValue;
    } catch (InitialisationException e) {
      throw e;
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ObjectBuilder createFieldParameterGroupBuilder(ParameterGroupDescriptor groupDescriptor,
                                                         List<ParameterModel> fieldParameters) {
    DefaultObjectBuilder groupBuilder =
        new DefaultObjectBuilder(groupDescriptor.getType().getDeclaringClass().get(), reflectionCache);

    fieldParameters.forEach(p -> {
      ValueResolver resolver = resolverSet.getResolvers().get(p.getName());
      if (resolver != null) {
        Optional<Field> memberField = getMemberField(p);
        if (memberField.isPresent()) {
          groupBuilder.addPropertyResolver(getMemberField(p).get(), resolver);
        } else {
          groupBuilder.addPropertyResolver(p.getName(), resolver);
        }
      }
    });
    return groupBuilder;
  }

  private List<ParameterModel> getGroupsOfFieldParameters(ParameterGroupModel group) {
    return group.getParameterModels().stream()
        .filter(p -> p.getModelProperty(FieldOperationParameterModelProperty.class).isPresent())
        .collect(toList());
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
      return new PayloadTargetReturnDelegate(target, componentModel, cursorProviderFactory, muleContext);
    }
    return new TargetReturnDelegate(target, targetValue, componentModel, expressionManager, cursorProviderFactory, muleContext);
  }

  protected ValueReturnDelegate getValueReturnDelegate() {
    return new ValueReturnDelegate(componentModel, cursorProviderFactory, muleContext);
  }

  protected boolean isTargetPresent() {
    if (isBlank(target)) {
      return false;
    }

    if (muleContext.getExpressionManager().isExpression(target)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 componentModel.getName(),
                                                 "an expression", TARGET_PARAMETER_NAME));
    } else if (!muleContext.getExpressionManager().isExpression(targetValue)) {
      throw new IllegalOperationException(format(INVALID_TARGET_MESSAGE, getLocation().getRootContainerName(),
                                                 componentModel.getName(), "something that is not an expression",
                                                 TARGET_VALUE_PARAMETER_NAME));
    }

    return true;
  }

  protected boolean isAsync() {
    if (!requiresConfig()) {
      return false;
    }

    if (usesDynamicConfiguration()) {
      return true;
    } else {
      Optional<ConfigurationInstance> staticConfig = getStaticConfiguration();
      if (staticConfig.isPresent()) {
        return getRetryPolicyTemplate(staticConfig).isEnabled();
      }
    }

    return true;
  }

  @Override
  public void doStart() throws MuleException {
    startIfNeeded(componentExecutor);

    if (ownedProcessingStrategy) {
      startIfNeeded(processingStrategy);
    }
    outerFluxCompletionScheduler = muleContext.getSchedulerService().ioScheduler(muleContext.getSchedulerBaseConfig()
        .withMaxConcurrentTasks(1).withName(toString() + ".outer.flux."));

    startInnerFlux();
  }

  @Override
  public void doStop() throws MuleException {
    stopIfNeeded(componentExecutor);
    stopInnerFlux();

    if (ownedProcessingStrategy) {
      stopIfNeeded(processingStrategy);
    }

    if (outerFluxCompletionScheduler != null) {
      outerFluxCompletionScheduler.stop();
      outerFluxCompletionScheduler = null;
    }
  }

  private void outerPublisherSubscribedTo() {
    activeOuterPublishersCount.getAndIncrement();
  }

  private void outerPublisherTerminated() {
    if (activeOuterPublishersCount.decrementAndGet() == 0) {
      stopInnerFlux();
    }
  }

  private void stopInnerFlux() {
    if (fluxSupplier != null) {
      synchronized (fluxSupplierDisposeLock) {
        if (fluxSupplier != null) {
          fluxSupplier.dispose();
          fluxSupplier = null;
        }
      }
    }
  }

  @Override
  public void doDispose() {
    disposeIfNeeded(componentExecutor, LOGGER);
    if (ownedProcessingStrategy) {
      disposeIfNeeded(processingStrategy, LOGGER);
    }
    initialised = false;
  }

  protected ExecutionMediator createExecutionMediator() {
    return new DefaultExecutionMediator(extensionModel, componentModel, createInterceptorChain(), errorTypeRepository);
  }

  protected InterceptorChain createInterceptorChain() {
    InterceptorChain.Builder chainBuilder = InterceptorChain.builder();

    if (componentModel instanceof ConnectableComponentModel) {
      if (((ConnectableComponentModel) componentModel).requiresConnection()) {
        addConnectionInterceptors(chainBuilder);
      }
    }

    return chainBuilder.build();
  }

  private void addConnectionInterceptors(InterceptorChain.Builder chainBuilder) {
    chainBuilder.addInterceptor(new ConnectionInterceptor(extensionConnectionSupplier));

    addCursorResetInterceptor(chainBuilder);
  }

  private void addCursorResetInterceptor(InterceptorChain.Builder chainBuilder) {
    List<String> streamParams = new ArrayList<>(5);
    componentModel.getAllParameterModels().forEach(
                                                   p -> getType(p.getType(), getClassLoader(extensionModel))
                                                       .filter(clazz -> InputStream.class.isAssignableFrom(clazz)
                                                           || Iterator.class.isAssignableFrom(clazz))
                                                       .ifPresent(clazz -> streamParams.add(p.getName())));

    if (!streamParams.isEmpty()) {
      chainBuilder.addInterceptor(new CursorResetInterceptor(streamParams));
    }
  }

  /**
   * Validates that the {@link #componentModel} is valid for the given {@code configurationProvider}
   *
   * @throws IllegalOperationException If the validation fails
   */
  @Override
  protected abstract void validateOperationConfiguration(ConfigurationProvider configurationProvider);

  @Override
  protected ParameterValueResolver getParameterValueResolver() {
    CoreEvent event = getNullEvent(muleContext);
    try (ValueResolvingContext ctx = ValueResolvingContext.builder(event, expressionManager).build()) {
      LazyExecutionContext executionContext = new LazyExecutionContext<>(resolverSet, componentModel, extensionModel, ctx);
      return new OperationParameterValueResolver(executionContext, resolverSet, reflectionCache, expressionManager);
    } finally {
      if (event != null) {
        ((BaseEventContext) event.getContext()).success();
      }
    }
  }

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
    if (componentExecutor instanceof OperationArgumentResolverFactory) {
      ExecutionContextAdapter<T> delegateExecutionContext = createExecutionContext(eventBuilder.build());
      PrecalculatedExecutionContextAdapter executionContext = new PrecalculatedExecutionContextAdapter(delegateExecutionContext);

      final DefaultExecutionMediator mediator = (DefaultExecutionMediator) executionMediator;
      Throwable throwable = mediator.applyBeforeInterceptors(executionContext);
      if (throwable == null) {
        final Map<String, Supplier<Object>> resolvedArguments = ((OperationArgumentResolverFactory<T>) componentExecutor)
            .createArgumentResolver(componentModel)
            .apply(executionContext);
        afterConfigurer.accept(resolvedArguments, executionContext);
        executionContext.changeEvent(eventBuilder.build());
      } else {
        throw new DefaultMuleException("Interception execution for operation not ok", throwable);
      }
    }
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
    try (ValueResolvingContext context = ValueResolvingContext.builder(event, expressionManager)
        .withConfig(configuration).build()) {
      return resolverSet.resolve(context).asMap();
    }
  }

  private boolean isInterceptedComponent(ComponentLocation location, InternalEvent event) {
    final Component component = event.getInternalParameter(INTERCEPTION_COMPONENT);
    if (component != null) {
      return location.equals(component.getLocation());
    }
    return false;
  }

  @Override
  public String toString() {
    return getLocation() != null ? getLocation().getLocation() : super.toString();
  }
}
