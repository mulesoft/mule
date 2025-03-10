/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactType;
import static org.mule.runtime.core.internal.util.CompositeClassLoader.from;
import static org.mule.runtime.core.privileged.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.runtime.module.artifact.api.classloader.RegionClassLoader.getNearestRegion;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getMutableConfigurationStats;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isConnectedStreamingOperation;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.NULL_THROWABLE_CONSUMER;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.shouldRetry;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentThreadingProfilingEventContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.exception.ExceptionHandlerManager;
import org.mule.runtime.module.extension.internal.runtime.exception.ModuleExceptionHandler;
import org.mule.runtime.module.extension.internal.runtime.execution.interceptor.InterceptorChain;
import org.mule.runtime.tracer.api.component.ComponentTracer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;

/**
 * Default implementation of {@link ExecutionMediator}.
 * <p>
 * It also inspects the {@link ConfigurationStats} obtained from the {@link ConfigurationDeclaration} in the {@code context}. If
 * the stats class implements the {@link MutableConfigurationStats} interface, then
 * {@link MutableConfigurationStats#addInflightOperation()} and {@link MutableConfigurationStats#discountInflightOperation()} are
 * guaranteed to be called, whatever the operation's outcome.
 * <p>
 * In case of operation failure, it will execute the {@link Interceptor#onError(ExecutionContext, Throwable)} method of all the
 * available interceptors. If the operation fails with {@link ConnectionException}, then a retry might be attempted depending on
 * the configured {@link RetryPolicyTemplate}. Notice that if a retry is attempted, the entire cycle of interception (before,
 * onSuccess/interceptError, after) will be fired again.
 *
 * @since 4.0
 */
public final class DefaultExecutionMediator<M extends ComponentModel> implements ExecutionMediator<M> {

  private final ExceptionHandlerManager exceptionEnricherManager;
  private final InterceptorChain interceptorChain;
  private final ExecutionTemplate<?> defaultExecutionTemplate = callback -> callback.process();
  private final ModuleExceptionHandler moduleExceptionHandler;
  private final MuleConfiguration muleConfiguration;
  private final NotificationDispatcher notificationDispatcher;
  private final ResultTransformer resultTransformer;
  private final ClassLoader executionClassLoader;
  private final ComponentModel operationModel;
  private final ProfilingDataProducer<ComponentThreadingProfilingEventContext, CoreEvent> threadReleaseDataProducer;
  private final ComponentTracer<CoreEvent> operationComponentTracer;

  private static final Logger LOGGER = getLogger(DefaultExecutionMediator.class);

  public DefaultExecutionMediator(ExtensionModel extensionModel,
                                  M operationModel,
                                  InterceptorChain interceptorChain,
                                  ErrorTypeRepository typeRepository,
                                  ClassLoader executionClassLoader,
                                  MuleConfiguration muleConfiguration,
                                  NotificationDispatcher notificationDispatcher,
                                  ResultTransformer resultTransformer,
                                  ProfilingDataProducer<ComponentThreadingProfilingEventContext, CoreEvent> threadReleaseDataProducer,
                                  ComponentTracer<CoreEvent> operationExecutionTracer,
                                  boolean suppressErrors) {
    this.interceptorChain = interceptorChain;
    this.exceptionEnricherManager = new ExceptionHandlerManager(extensionModel, operationModel, typeRepository);
    this.moduleExceptionHandler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
    this.muleConfiguration = requireNonNull(muleConfiguration);
    this.notificationDispatcher = notificationDispatcher;
    this.resultTransformer = resultTransformer;
    this.operationModel = operationModel;

    // The effective execution ClassLoader will be a composition with the extension ClassLoader being used first and
    // then the default execution ClassLoader which may depend on the execution context.
    // This is important for cases where the extension does not belong to the region of the operation, see MULE-18159.
    final ClassLoader extensionClassLoader = getClassLoader(extensionModel);
    executionClassLoader = getNearestRegion(executionClassLoader);
    if (executionClassLoader != null && !executionClassLoader.equals(extensionClassLoader)) {
      this.executionClassLoader = from(extensionClassLoader, executionClassLoader);
    } else {
      this.executionClassLoader = extensionClassLoader;
    }

    this.threadReleaseDataProducer = threadReleaseDataProducer;
    this.operationComponentTracer = operationExecutionTracer;
  }

  /**
   * Executes the operation per the specification in these classes javadoc
   *
   * @param executor an {@link CompletableComponentExecutor}
   * @param context  the {@link ExecutionContextAdapter} for the {@code executor} to use
   * @return the operation's result
   * @throws Exception if the operation or a {@link Interceptor#before(ExecutionContext)} invocation fails
   */
  @Override
  public void execute(CompletableComponentExecutor<M> executor,
                      ExecutionContextAdapter<M> context,
                      ExecutorCallback callback) {
    try (DeferredExecutorCallback deferredCallback =
        new DeferredExecutorCallback(getDelegateExecutorCallback(getStats(context), callback, context))) {
      withExecutionTemplate((ExecutionContextAdapter<ComponentModel>) context, () -> {
        executeWithInterceptors(executor, context, deferredCallback);
        return null;
      });
    } catch (Exception e) {
      callback.error(e);
    } catch (Throwable t) {
      callback.error(wrapFatal(t));
    }
  }

  private MutableConfigurationStats getStats(ExecutionContextAdapter<M> context) {
    final MutableConfigurationStats stats = getMutableConfigurationStats(context);
    if (stats != null) {
      stats.addActiveComponent();
      stats.addInflightOperation();
    }

    return stats;
  }

  private ExecutorCallback getDelegateExecutorCallback(final MutableConfigurationStats stats,
                                                       ExecutorCallback callback,
                                                       ExecutionContextAdapter<M> context) {
    return new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        if (stats != null) {
          if (!isConnectedStreamingOperation(operationModel)) {
            stats.discountActiveComponent();
          }
          stats.discountInflightOperation();
        }
        try {
          interceptorChain.onSuccess(context, value);
          callback.complete(value);
        } catch (Throwable t) {
          try {
            t = handleError(t, context);
          } finally {
            callback.error(t);
          }
        }
      }

      @Override
      public void error(Throwable t) {
        try {
          t = handleError(t, context);
        } finally {
          if (stats != null) {
            stats.discountInflightOperation();
            stats.discountActiveComponent();
          }
          callback.error(t);
        }
      }
    };
  }

  private void executeWithRetry(ExecutionContextAdapter<M> context,
                                RetryPolicyTemplate retryPolicy,
                                Consumer<ExecutorCallback> executeCommand,
                                ExecutorCallback callback) {

    retryPolicy.applyPolicy(() -> {
      CompletableFuture<Object> future = new CompletableFuture<>();
      executeCommand.accept(new FutureExecutionCallbackAdapter(future));
      return future;
    },
                            e -> shouldRetry(e, context),
                            e -> interceptorChain.onError(context, e),
                            NULL_THROWABLE_CONSUMER,
                            identity(),
                            context.getCurrentScheduler())
        .whenComplete((v, e) -> {
          if (e != null) {
            callback.error(e);
          } else {
            callback.complete(v);
          }
        });
  }

  private void executeWithInterceptors(CompletableComponentExecutor<M> executor,
                                       ExecutionContextAdapter<M> context,
                                       ExecutorCallback executorCallback) {
    RetryPolicyTemplate retryPolicy = context.getRetryPolicyTemplate().orElse(null);
    if (retryPolicy != null && retryPolicy.isEnabled()) {
      executeWithRetry(context, retryPolicy, callback -> executeCommand(executor, context, callback), executorCallback);
    } else {
      executeCommand(executor, context, executorCallback);
    }
  }

  private void executeCommand(CompletableComponentExecutor<M> executor,
                              ExecutionContextAdapter<M> context,
                              ExecutorCallback callback) {
    Throwable t = interceptorChain.before(context, callback);
    if (t == null) {
      if (resultTransformer != null) {
        callback = new TransformingExecutionCallbackDecorator(callback, context, resultTransformer);
      }
      final Thread currentThread = currentThread();
      final ClassLoader currentClassLoader = currentThread.getContextClassLoader();
      setContextClassLoader(currentThread, currentClassLoader, executionClassLoader);
      try {
        operationComponentTracer.startSpan(context.getEvent());
        executor.execute(context, new TracedOperationExecutionCallback(context, operationComponentTracer, callback));
      } finally {
        profileThreadRelease(context);
        setContextClassLoader(currentThread, executionClassLoader, currentClassLoader);
      }
    }
  }

  private void profileThreadRelease(ExecutionContextAdapter<M> context) {
    String threadName = currentThread().getName();
    String artifactId = muleConfiguration.getId();
    String artifactType = getArtifactType(context.getMuleContext());
    threadReleaseDataProducer.triggerProfilingEvent(context
        .getEvent(), event -> new DefaultComponentThreadingProfilingEventContext(event, context
            .getComponent().getLocation(), threadName, artifactId, artifactType, currentTimeMillis()));
  }

  private Throwable handleError(Throwable original, ExecutionContextAdapter context) {
    try {
      Throwable handled = exceptionEnricherManager.process(original);
      handled = moduleExceptionHandler.processException(handled);
      return interceptorChain.onError(context, handled);
      // TODO: MULE-19723
    } catch (Exception handlingException) {
      // Errors will be logged and suppressed from the execution (a different error is already being handled)
      LOGGER.error("An exception has been thrown during the operation error handling", handlingException);
      return original;
    }
  }

  Throwable applyBeforeInterceptors(ExecutionContextAdapter context) {
    try {
      return withExecutionTemplate(context, () -> {
        RetryPolicyTemplate retryPolicy = (RetryPolicyTemplate) context.getRetryPolicyTemplate().orElse(null);
        if (retryPolicy != null && retryPolicy.isEnabled()) {
          CompletableFuture<Throwable> result = new CompletableFuture<>();

          executeWithRetry(context, retryPolicy, callback -> {
            final Throwable t = interceptorChain.before(context, callback);
            if (t == null) {
              result.complete(null);
            }
          },
                           new ExecutorCallback() {

                             @Override
                             public void complete(Object value) {
                               result.complete((Throwable) value);
                             }

                             @Override
                             public void error(Throwable e) {
                               result.completeExceptionally(e);
                             }

                           });

          return result.get();
        } else {
          return interceptorChain.before(context, null);
        }
      });
    } catch (Exception e) {
      return e;
    }
  }

  void applyAfterInterceptors(ExecutionContext executionContext) {
    interceptorChain.abort(executionContext);
  }

  private <T> T withExecutionTemplate(ExecutionContextAdapter<ComponentModel> context, ExecutionCallback<T> callback)
      throws Exception {
    if (context.getTransactionConfig().isPresent()) {
      return ((ExecutionTemplate<T>) createTransactionalExecutionTemplate(muleConfiguration,
                                                                          notificationDispatcher,
                                                                          context.getTransactionConfig().orElseThrow()))
                                                                              .execute(callback);
    } else {
      return ((ExecutionTemplate<T>) defaultExecutionTemplate)
          .execute(callback);
    }
  }

  private static class TransformingExecutionCallbackDecorator<M extends ComponentModel> implements ExecutorCallback {

    private final ExecutorCallback delegate;
    private final ExecutionContextAdapter<M> executionContext;
    private final ResultTransformer resultTransformer;

    public TransformingExecutionCallbackDecorator(ExecutorCallback delegate,
                                                  ExecutionContextAdapter<M> executionContext,
                                                  ResultTransformer resultTransformer) {
      this.delegate = delegate;
      this.executionContext = executionContext;
      this.resultTransformer = resultTransformer;
    }

    @Override
    public void complete(Object value) {
      try {
        delegate.complete(resultTransformer.apply(executionContext, value));
      } catch (Exception e) {
        delegate.error(e);
      }
    }

    @Override
    public void error(Throwable e) {
      delegate.error(e);
    }
  }

}
