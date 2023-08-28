/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory.DUMMY_COMPONENT_TRACER_INSTANCE;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.runtime.module.extension.internal.runtime.client.NullComponent.NULL_COMPONENT;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.evaluate;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;
import static org.mule.runtime.module.extension.internal.util.InterceptorChainUtils.createConnectionInterceptorsChain;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getPagingResultTransformer;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.supportsOAuth;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.GET_CONNECTION_SPAN_NAME;

import static java.util.Optional.empty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.streaming.CursorProviderDecorator;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.ResultTransformer;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.result.ValueReturnDelegate;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;

/**
 * {@link ExtensionsClient} delegate class for executing extension operations
 *
 * @since 4.5.0
 */
public class OperationClient implements Lifecycle {

  private static final Logger LOGGER = getLogger(OperationClient.class);
  private static final NullProfilingDataProducer NULL_PROFILING_DATA_PRODUCER = new NullProfilingDataProducer();

  private final ExecutionMediator<OperationModel> mediator;
  private final ComponentExecutorResolver executorResolver;
  private final ValueReturnDelegate returnDelegate;
  private final StreamingManager streamingManager;
  private final ExpressionManager expressionManager;
  private final ReflectionCache reflectionCache;
  private final MuleContext muleContext;

  public static OperationClient from(OperationKey key,
                                     ExtensionManager extensionManager,
                                     ExpressionManager expressionManager,
                                     ExtensionConnectionSupplier extensionConnectionSupplier,
                                     ErrorTypeRepository errorTypeRepository,
                                     StreamingManager streamingManager,
                                     ReflectionCache reflectionCache,
                                     ComponentTracerFactory<CoreEvent> componentTracerFactory,
                                     MuleContext muleContext) {

    return new OperationClient(
                               createExecutionMediator(
                                                       key,
                                                       extensionConnectionSupplier,
                                                       errorTypeRepository,
                                                       reflectionCache,
                                                       componentTracerFactory,
                                                       muleContext),
                               ComponentExecutorResolver.from(key, extensionManager, expressionManager, reflectionCache,
                                                              muleContext),
                               new ValueReturnDelegate(key.getOperationModel(), muleContext),
                               streamingManager,
                               expressionManager,
                               reflectionCache,
                               muleContext);
  }

  private OperationClient(ExecutionMediator<OperationModel> mediator,
                          ComponentExecutorResolver executorResolver,
                          ValueReturnDelegate returnDelegate,
                          StreamingManager streamingManager,
                          ExpressionManager expressionManager,
                          ReflectionCache reflectionCache,
                          MuleContext muleContext) {
    this.mediator = mediator;
    this.executorResolver = executorResolver;
    this.returnDelegate = returnDelegate;
    this.streamingManager = streamingManager;
    this.expressionManager = expressionManager;
    this.reflectionCache = reflectionCache;
    this.muleContext = muleContext;
  }

  public <T, A> CompletableFuture<Result<T, A>> execute(OperationKey key, DefaultOperationParameterizer parameterizer) {
    boolean shouldCompleteEvent = false;
    CoreEvent contextEvent = parameterizer.getContextEvent().orElse(null);
    if (contextEvent == null) {
      contextEvent = getNullEvent(muleContext);
      shouldCompleteEvent = true;
    }

    OperationModel operationModel = key.getOperationModel();
    Optional<ConfigurationInstance> configurationInstance = getConfigurationInstance(key, contextEvent);

    final Map<String, Object> resolvedParams =
        resolveOperationParameters(operationModel, configurationInstance, parameterizer, contextEvent);
    CursorProviderFactory<Object> cursorProviderFactory = parameterizer.getCursorProviderFactory(streamingManager);

    ExecutionContextAdapter<OperationModel> context = new DefaultExecutionContext<>(
                                                                                    key.getExtensionModel(),
                                                                                    configurationInstance,
                                                                                    resolvedParams,
                                                                                    operationModel,
                                                                                    contextEvent,
                                                                                    cursorProviderFactory,
                                                                                    streamingManager,
                                                                                    NULL_COMPONENT,
                                                                                    parameterizer.getRetryPolicyTemplate(),
                                                                                    IMMEDIATE_SCHEDULER,
                                                                                    empty(),
                                                                                    muleContext);

    return doExecute(context, shouldCompleteEvent);
  }

  private <T, A> CompletableFuture<Result<T, A>> doExecute(ExecutionContextAdapter<OperationModel> ctx,
                                                           boolean shouldCompleteEvent) {

    CompletableFuture<Result<T, A>> future = new CompletableFuture<>();
    ExecutorCallback callback = new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        EventCompletingValue<Result<T, A>> result = asEventCompletingResult(value, ctx, shouldCompleteEvent);

        try {
          future.complete(result.value);
        } finally {
          if (result.shouldCompleteEvent) {
            ((BaseEventContext) ctx.getEvent().getContext()).success();
          }
        }
      }

      @Override
      public void error(Throwable e) {
        if (!(e instanceof MessagingException)) {
          e = new MessagingException(ctx.getEvent(), e);
        }
        try {
          future.completeExceptionally(e);
        } finally {
          if (shouldCompleteEvent) {
            ((BaseEventContext) ctx.getEvent().getContext()).error(e);
          }
        }
      }
    };

    mediator.execute(executorResolver.resolveExecutor(ctx.getParameters()), ctx, callback);
    return future;
  }

  private Map<String, Object> resolveOperationParameters(OperationModel operationModel,
                                                         Optional<ConfigurationInstance> configurationInstance,
                                                         DefaultOperationParameterizer parameterizer,
                                                         CoreEvent event) {
    ComponentParameterization.Builder<OperationModel> paramsBuilder = ComponentParameterization.builder(operationModel);
    parameterizer.setValuesOn(paramsBuilder);

    ResolverSet resolverSet;
    try {
      resolverSet = getResolverSetFromComponentParameterization(
                                                                paramsBuilder.build(),
                                                                muleContext,
                                                                true,
                                                                reflectionCache,
                                                                expressionManager,
                                                                "");

      resolverSet.initialise();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(e.getMessage()), e);
    }

    return evaluate(resolverSet, configurationInstance, event);
  }

  private <T, A> EventCompletingValue<Result<T, A>> asEventCompletingResult(Object value,
                                                                            ExecutionContextAdapter<OperationModel> context,
                                                                            boolean shouldCompleteEvent) {

    Message message = returnDelegate.asReturnValue(value, context).getMessage();
    TypedValue payload = message.getPayload();
    TypedValue attributes = message.getAttributes();

    EventCompletingValue<Object> completingPayload = asEventCompletingValue(payload.getValue(), context, shouldCompleteEvent);
    shouldCompleteEvent = shouldCompleteEvent && completingPayload.shouldCompleteEvent;

    EventCompletingValue<Object> completingAttributes =
        asEventCompletingValue(attributes.getValue(), context, shouldCompleteEvent);
    shouldCompleteEvent = shouldCompleteEvent && completingAttributes.shouldCompleteEvent;

    Result<T, A> result = (Result<T, A>) Result.builder()
        .output(completingPayload.value)
        .mediaType(payload.getDataType().getMediaType())
        .attributes(completingAttributes.value)
        .attributesMediaType(attributes.getDataType().getMediaType())
        .build();

    return new EventCompletingValue<>(result, shouldCompleteEvent);
  }

  private EventCompletingValue<Object> asEventCompletingValue(Object value, ExecutionContextAdapter ctx,
                                                              boolean shouldCompleteEvent) {
    if (shouldCompleteEvent) {
      if (value instanceof CursorIteratorProvider) {
        return new EventCompletingValue<>(new EventCompletingCursorIteratorProviderDecorator((CursorIteratorProvider) value,
                                                                                             ctx.getEvent()),
                                          false);
      } else if (value instanceof CursorStreamProvider) {
        return new EventCompletingValue<>(new EventCompletingCursorStreamProviderDecorator((CursorStreamProvider) value,
                                                                                           ctx.getEvent()),
                                          false);
      }
    }

    return new EventCompletingValue<>(value, shouldCompleteEvent);
  }

  private Optional<ConfigurationInstance> getConfigurationInstance(OperationKey key, CoreEvent contextEvent) {
    return key.getConfigurationProvider(contextEvent).map(config -> config.get(contextEvent));
  }


  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(mediator, true, muleContext);
    initialiseIfNeeded(executorResolver, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(mediator);
    startIfNeeded(executorResolver);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(mediator);
    stopIfNeeded(executorResolver);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(mediator, LOGGER);
    disposeIfNeeded(executorResolver, LOGGER);
  }

  private static ExecutionMediator<OperationModel> createExecutionMediator(
                                                                           OperationKey key,
                                                                           ExtensionConnectionSupplier extensionConnectionSupplier,
                                                                           ErrorTypeRepository errorTypeRepository,
                                                                           ReflectionCache reflectionCache,
                                                                           ComponentTracerFactory<CoreEvent> componentTracerFactory,
                                                                           MuleContext muleContext) {

    final ExtensionModel extensionModel = key.getExtensionModel();
    final OperationModel operationModel = key.getOperationModel();
    ExecutionMediator<OperationModel> mediator = new DefaultExecutionMediator<>(
                                                                                extensionModel,
                                                                                operationModel,
                                                                                createConnectionInterceptorsChain(extensionModel,
                                                                                                                  operationModel,
                                                                                                                  extensionConnectionSupplier,
                                                                                                                  reflectionCache,
                                                                                                                  DUMMY_COMPONENT_TRACER_INSTANCE),
                                                                                errorTypeRepository,
                                                                                muleContext.getExecutionClassLoader(),
                                                                                getResultTransformer(extensionConnectionSupplier,
                                                                                                     extensionModel,
                                                                                                     operationModel,
                                                                                                     componentTracerFactory),
                                                                                NULL_PROFILING_DATA_PRODUCER,
                                                                                getOperationConnectionTracer(componentTracerFactory),
                                                                                false);

    try {
      initialiseIfNeeded(mediator, true, muleContext);
      startIfNeeded(mediator);

      return mediator;
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create mediator for operation " + key), e);
    }
  }

  private static ComponentTracer<CoreEvent> getOperationConnectionTracer(ComponentTracerFactory<CoreEvent> componentTracerFactory) {
    return componentTracerFactory
        .fromComponent(NULL_COMPONENT,
                       GET_CONNECTION_SPAN_NAME, "");
  }

  private static ResultTransformer getResultTransformer(ExtensionConnectionSupplier extensionConnectionSupplier,
                                                        ExtensionModel extensionModel, OperationModel operationModel,
                                                        ComponentTracerFactory<CoreEvent> componentTracerFactory) {
    return getPagingResultTransformer(operationModel,
                                      extensionConnectionSupplier,
                                      supportsOAuth(extensionModel), getOperationConnectionTracer(componentTracerFactory))
                                          .orElse(null);
  }

  private static abstract class EventCompletingCursorProviderDecorator<T extends Cursor> extends CursorProviderDecorator<T> {

    private final CoreEvent event;

    private EventCompletingCursorProviderDecorator(CursorProvider delegate, CoreEvent event) {
      super(delegate);
      this.event = event;
    }

    @Override
    public void close() {
      try {
        super.close();
      } finally {
        ((BaseEventContext) event.getContext()).success();
      }
    }
  }


  private static class EventCompletingCursorStreamProviderDecorator
      extends EventCompletingCursorProviderDecorator<CursorStream> implements CursorStreamProvider {

    public EventCompletingCursorStreamProviderDecorator(CursorStreamProvider delegate, CoreEvent event) {
      super(delegate, event);
    }
  }


  private static class EventCompletingCursorIteratorProviderDecorator
      extends EventCompletingCursorProviderDecorator<CursorIterator> implements CursorIteratorProvider {

    public EventCompletingCursorIteratorProviderDecorator(CursorIteratorProvider delegate, CoreEvent event) {
      super(delegate, event);
    }
  }


  private static class EventCompletingValue<T> {

    private final T value;
    private final boolean shouldCompleteEvent;

    private EventCompletingValue(T value, boolean shouldCompleteEvent) {
      this.value = value;
      this.shouldCompleteEvent = shouldCompleteEvent;
    }
  }


  private static class NullProfilingDataProducer
      implements ProfilingDataProducer<ComponentThreadingProfilingEventContext, CoreEvent> {

    private NullProfilingDataProducer() {}

    @Override
    public void triggerProfilingEvent(ComponentThreadingProfilingEventContext profilerEventContext) {

    }

    @Override
    public void triggerProfilingEvent(CoreEvent sourceData,
                                      Function<CoreEvent, ComponentThreadingProfilingEventContext> transformation) {

    }
  }
}
