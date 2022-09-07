/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getPagingResultTransformer;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.supportsOAuth;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.createReconnectionInterceptorsChain;
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
import org.mule.runtime.core.internal.streaming.CursorProviderDecorator;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.result.ValueReturnDelegate;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;

public class OperationClient implements Lifecycle {

  private static Logger LOGGER = getLogger(OperationClient.class);
  private static final NullProfilingDataProducer NULL_PROFILING_DATA_PRODUCER = new NullProfilingDataProducer();

  private final ExecutionMediator<OperationModel> mediator;
  private final ComponentExecutorResolver executorResolver;
  private final ValueReturnDelegate returnDelegate;
  private final MuleContext muleContext;

  public static OperationClient from(OperationKey key,
                                     ExtensionManager extensionManager,
                                     ExpressionManager expressionManager,
                                     ExtensionConnectionSupplier extensionConnectionSupplier,
                                     ErrorTypeRepository errorTypeRepository,
                                     ReflectionCache reflectionCache,
                                     MuleContext muleContext) {

    return new OperationClient(
                               createExecutionMediator(
                                                       key,
                                                       extensionConnectionSupplier,
                                                       errorTypeRepository,
                                                       reflectionCache,
                                                       muleContext),
                               ComponentExecutorResolver.from(key, extensionManager, expressionManager, reflectionCache,
                                                              muleContext),
                               new ValueReturnDelegate(key.getOperationModel(), muleContext),
                               muleContext);
  }

  private OperationClient(ExecutionMediator<OperationModel> mediator,
                          ComponentExecutorResolver executorResolver,
                          ValueReturnDelegate returnDelegate,
                          MuleContext muleContext) {
    this.mediator = mediator;
    this.executorResolver = executorResolver;
    this.returnDelegate = returnDelegate;
    this.muleContext = muleContext;
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

  public <T, A> CompletableFuture<Result<T, A>> execute(ExecutionContextAdapter<OperationModel> ctx,
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

  private static ExecutionMediator<OperationModel> createExecutionMediator(
                                                                           OperationKey key,
                                                                           ExtensionConnectionSupplier extensionConnectionSupplier,
                                                                           ErrorTypeRepository errorTypeRepository,
                                                                           ReflectionCache reflectionCache,
                                                                           MuleContext muleContext) {

    final ExtensionModel extensionModel = key.getExtensionModel();
    final OperationModel operationModel = key.getOperationModel();
    ExecutionMediator<OperationModel> mediator = new DefaultExecutionMediator<>(
                                                                                extensionModel,
                                                                                operationModel,
                                                                                createReconnectionInterceptorsChain(extensionModel,
                                                                                                                    operationModel,
                                                                                                                    extensionConnectionSupplier,
                                                                                                                    reflectionCache),
                                                                                errorTypeRepository,
                                                                                muleContext.getExecutionClassLoader(),
                                                                                getPagingResultTransformer(operationModel,
                                                                                                           extensionConnectionSupplier,
                                                                                                           supportsOAuth(extensionModel))
                                                                                                               .orElse(null),
                                                                                NULL_PROFILING_DATA_PRODUCER);

    try {
      initialiseIfNeeded(mediator, true, muleContext);
      startIfNeeded(mediator);

      return mediator;
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create mediator for operation " + key), e);
    }
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
