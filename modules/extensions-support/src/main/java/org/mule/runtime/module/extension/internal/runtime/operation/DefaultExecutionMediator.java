/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.function.Function.identity;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.util.func.CheckedBiFunction;
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
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
  private final ResultTransformer resultTransformer;
  private final ClassLoader extensionClassLoader;


  @FunctionalInterface
  public interface ResultTransformer extends CheckedBiFunction<ExecutionContextAdapter, Object, Object> {

  }

  public DefaultExecutionMediator(ExtensionModel extensionModel,
                                  M operationModel,
                                  InterceptorChain interceptorChain,
                                  ErrorTypeRepository typeRepository) {
    this(extensionModel, operationModel, interceptorChain, typeRepository, null);
  }

  public DefaultExecutionMediator(ExtensionModel extensionModel,
                                  M operationModel,
                                  InterceptorChain interceptorChain,
                                  ErrorTypeRepository typeRepository,
                                  ResultTransformer resultTransformer) {
    this.interceptorChain = interceptorChain;
    this.exceptionEnricherManager = new ExceptionHandlerManager(extensionModel, operationModel, typeRepository);
    this.moduleExceptionHandler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository);
    this.resultTransformer = resultTransformer;
    extensionClassLoader = getClassLoader(extensionModel);
  }

  /**
   * Executes the operation per the specification in this classes' javadoc
   *
   * @param executor an {@link CompletableComponentExecutor}
   * @param context the {@link ExecutionContextAdapter} for the {@code executor} to use
   * @return the operation's result
   * @throws Exception if the operation or a {@link Interceptor#before(ExecutionContext)} invokation fails
   */
  @Override
  public void execute(CompletableComponentExecutor<M> executor,
                      ExecutionContextAdapter<M> context,
                      ExecutorCallback callback) {

    final MutableConfigurationStats stats = getMutableConfigurationStats(context);
    if (stats != null) {
      stats.addInflightOperation();
    }

    try {
      getExecutionTemplate((ExecutionContextAdapter<ComponentModel>) context).execute(() -> {
        executeWithInterceptors(executor, context, stats, callback);
        return null;
      });
    } catch (Exception e) {
      callback.error(e);
    } catch (Throwable t) {
      callback.error(wrapFatal(t));
    }
  }

  private class FutureExecutionCallbackDecorator implements ExecutorCallback {

    private final CompletableFuture<Object> future;
    private final Reference<Object> valueReference = new Reference<>();

    private FutureExecutionCallbackDecorator(CompletableFuture<Object> future) {
      this.future = future;
    }

    @Override
    public void complete(Object value) {
      valueReference.set(value);
      future.complete(value);
    }

    @Override
    public void error(Throwable e) {
      future.completeExceptionally(e);
    }
  }

  private void executeWithRetry(ExecutionContextAdapter<M> context,
                                RetryPolicyTemplate retryPolicy,
                                Consumer<ExecutorCallback> executeCommand,
                                ExecutorCallback callback) {

    retryPolicy.applyPolicy(() -> {
      CompletableFuture<Object> future = new CompletableFuture<>();
      executeCommand.accept(new FutureExecutionCallbackDecorator(future));
      return future;
    },
                            e -> shouldRetry(e, context),
                            e -> interceptorChain.onError(context, e),
                            e -> {
                            },
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

  private boolean shouldRetry(Throwable t, ExecutionContextAdapter<M> context) {
    if (!extractConnectionException(t).isPresent()) {
      return false;
    }

    if (isTransactionActive()) {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();

      return !tx.hasResource(new ExtensionTransactionKey(context.getConfiguration().get()));
    }

    return true;
  }

  private void executeWithInterceptors(CompletableComponentExecutor<M> executor,
                                       ExecutionContextAdapter<M> context,
                                       MutableConfigurationStats stats,
                                       ExecutorCallback executorCallback) {

    ExecutorCallback callbackDelegate = new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        // after() method cannot be invoked in the finally. Needs to be explicitly called before completing the callback.
        // Race conditions appear otherwise, specially in connection pooling scenarios.
        try {
          if (resultTransformer != null) {
            value = resultTransformer.apply(context, value);
          }
          interceptorChain.onSuccess(context, value);
          executorCallback.complete(value);
        } catch (Throwable t) {
          try {
            t = handleError(t, context);
          } finally {
            executorCallback.error(t);
          }
        } finally {
          if (stats != null) {
            stats.discountInflightOperation();
          }
        }
      }

      @Override
      public void error(Throwable t) {
        try {
          t = handleError(t, context);
        } finally {
          try {
            executorCallback.error(t);
          } finally {
            if (stats != null) {
              stats.discountInflightOperation();
            }
          }
        }
      }
    };

    RetryPolicyTemplate retryPolicy = context.getRetryPolicyTemplate().orElse(null);
    if (retryPolicy != null && retryPolicy.isEnabled()) {
      executeWithRetry(context, retryPolicy, callback -> executeCommand(executor, context, callback), callbackDelegate);
    } else {
      executeCommand(executor, context, callbackDelegate);
    }
  }

  private void executeCommand(CompletableComponentExecutor<M> executor,
                              ExecutionContextAdapter<M> context,
                              ExecutorCallback callback) {
    Throwable t = interceptorChain.before(context, callback);
    if (t == null) {
      final Thread currentThread = Thread.currentThread();
      final ClassLoader currentClassLoader = currentThread.getContextClassLoader();
      setContextClassLoader(currentThread, currentClassLoader, extensionClassLoader);
      try {
        executor.execute(context, callback);
      } finally {
        setContextClassLoader(currentThread, extensionClassLoader, currentClassLoader);
      }
    }
  }

  private Throwable handleError(Throwable e, ExecutionContextAdapter context) {
    e = exceptionEnricherManager.process(e);
    e = moduleExceptionHandler.processException(e);
    e = interceptorChain.onError(context, e);

    return e;
  }

  Throwable applyBeforeInterceptors(ExecutionContextAdapter executionContext) {
    return interceptorChain.before(executionContext, null);
  }

  void applyAfterInterceptors(ExecutionContext executionContext) {
    interceptorChain.abort(executionContext);
  }

  private <T> ExecutionTemplate<T> getExecutionTemplate(ExecutionContextAdapter<ComponentModel> context) {
    if (context.getTransactionConfig().isPresent()) {
      return ((ExecutionTemplate<T>) createTransactionalExecutionTemplate(context.getMuleContext(),
                                                                          context.getTransactionConfig().get()));
    } else {
      return (ExecutionTemplate<T>) defaultExecutionTemplate;
    }
  }

  private MutableConfigurationStats getMutableConfigurationStats(ExecutionContext<M> context) {
    if (context.getConfiguration().isPresent()) {
      return (MutableConfigurationStats) (context.getConfiguration().get()).getStatistics();
    } else {
      return null;
    }
  }
}
