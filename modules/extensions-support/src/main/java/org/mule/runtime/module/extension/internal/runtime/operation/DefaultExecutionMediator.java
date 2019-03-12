/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.util.func.CheckedBiFunction;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.exception.ExceptionHandlerManager;
import org.mule.runtime.module.extension.internal.runtime.exception.ModuleExceptionHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExecutionMediator}.
 * <p>
 * If the given {@code context} implements the {@link Interceptable}, then its defined {@link Interceptor}s are properly executed
 * as well.
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
public final class DefaultExecutionMediator<M extends ComponentModel, T, A> implements ExecutionMediator<M> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExecutionMediator.class);

  private final ExceptionHandlerManager exceptionEnricherManager;
  private final ConnectionManagerAdapter connectionManager;
  private final ExecutionTemplate<?> defaultExecutionTemplate = callback -> callback.process();
  private final ModuleExceptionHandler moduleExceptionHandler;
  private final List<ValueTransformer> valueTransformers;

  private final RetryPolicyTemplate fallbackRetryPolicyTemplate = new NoRetryPolicyTemplate();


  @FunctionalInterface
  public interface ValueTransformer extends CheckedBiFunction<ExecutionContextAdapter, Object, Object> {

  }

  public DefaultExecutionMediator(ExtensionModel extensionModel,
                                  M operationModel,
                                  ConnectionManagerAdapter connectionManager,
                                  ErrorTypeRepository typeRepository,
                                  ValueTransformer... valueTransformers) {
    this.connectionManager = connectionManager;
    this.exceptionEnricherManager = new ExceptionHandlerManager(extensionModel, operationModel);
    this.moduleExceptionHandler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository);
    this.valueTransformers = valueTransformers != null ? asList(valueTransformers) : emptyList();
  }

  /**
   * Executes the operation per the specification in this classes' javadoc
   *
   * @param executor an {@link CompletableComponentExecutor}
   * @param context  the {@link ExecutionContextAdapter} for the {@code executor} to use
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
        executeWithInterceptors(executor, context, collectInterceptors(context, executor), stats, callback);
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

  private void executeWithoutRetry(Consumer<ExecutorCallback> executeCommand,
                                   BiConsumer<ExecutorCallback, Object> onSuccess,
                                   BiConsumer<ExecutorCallback, Throwable> onError,
                                   ExecutorCallback callback) {

    ExecutorCallback hack = new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        onSuccess.accept(callback, value);
      }

      @Override
      public void error(Throwable e) {
        onError.accept(callback, e);
      }
    };

    executeCommand.accept(hack);
  }

  private void executeWithRetry(ExecutionContextAdapter<M> context,
                                RetryPolicyTemplate retryPolicy,
                                List<Interceptor> executedInterceptors,
                                Consumer<ExecutorCallback> executeCommand,
                                BiConsumer<ExecutorCallback, Object> onSuccess,
                                BiConsumer<ExecutorCallback, Throwable> onError,
                                ExecutorCallback callback) {

    retryPolicy.applyPolicy(() -> {
      CompletableFuture<Object> future = new CompletableFuture<>();
      executeCommand.accept(new FutureExecutionCallbackDecorator(future));
      return future;
    },
                            e -> extractConnectionException(e).isPresent(),
                            e -> {
                              interceptError(context, e, executedInterceptors);
                              after(context, null, executedInterceptors);
                            },
                            e -> {
                            },
                            identity(),
                            context.getCurrentScheduler())
        .whenComplete((v, e) -> {
          if (e != null) {
            onError.accept(callback, e);
          } else {
            onSuccess.accept(callback, v);
          }
        });
  }

  private void executeWithInterceptors(CompletableComponentExecutor<M> executor,
                                       ExecutionContextAdapter<M> context,
                                       final List<Interceptor> interceptors,
                                       MutableConfigurationStats stats,
                                       ExecutorCallback executorCallback) {

    List<Interceptor> executedInterceptors = new ArrayList<>(interceptors.size());

    Consumer<ExecutorCallback> executeCommand = callback -> {
      // If the operation is retried, then the interceptors need to be executed again,
      executedInterceptors.clear();
      InterceptorsExecutionResult beforeExecutionResult = before(context, interceptors);
      if (beforeExecutionResult.isOk()) {
        executedInterceptors.addAll(interceptors);
        withContextClassLoader(getClassLoader(context.getExtensionModel()), () -> executor.execute(context, callback));
      } else {
        executedInterceptors.addAll(beforeExecutionResult.getExecutedInterceptors());
        callback.error(beforeExecutionResult.getThrowable());
      }
    };

    BiConsumer<ExecutorCallback, Object> onSuccess = (callback, value) -> {
      // after() method cannot be invoked in the finally. Needs to be explicitly called before completing the callback.
      // Race conditions appear otherwise, specially in connection pooling scenarios.
      try {
        value = transform(context, value);
        onSuccess(context, value, executedInterceptors);
        after(context, value, executedInterceptors);
        callback.complete(value);
      } catch (Throwable t) {
        try {
          t = handleError(t, context, executedInterceptors);
        } finally {
          try {
            after(context, value, executedInterceptors);
          } finally {
            callback.error(t);
          }
        }
      } finally {
        if (stats != null) {
          stats.discountInflightOperation();
        }
      }
    };

    BiConsumer<ExecutorCallback, Throwable> onError = (callback, t) -> {
      t = handleError(t, context, executedInterceptors);
      try {
        after(context, null, executedInterceptors);
      } finally {
        try {
          callback.error(t);
        } finally {
          if (stats != null) {
            stats.discountInflightOperation();
          }
        }
      }
    };

    RetryPolicyTemplate retryPolicy = getRetryPolicyTemplate(context);
    if (retryPolicy.isEnabled()) {
      executeWithRetry(context, retryPolicy, executedInterceptors, executeCommand, onSuccess, onError, executorCallback);
    } else {
      executeWithoutRetry(executeCommand, onSuccess, onError, executorCallback);
    }
  }

  private Throwable handleError(Throwable e, ExecutionContextAdapter context, List<Interceptor> interceptors) {
    return mapError(e, context, interceptors);
  }

  private Throwable mapError(Throwable e, ExecutionContextAdapter context, List<Interceptor> interceptors) {
    e = exceptionEnricherManager.process(e);
    e = moduleExceptionHandler.processException(e);
    e = interceptError(context, e, interceptors);

    return e;
  }

  private Object transform(ExecutionContextAdapter context, Object value) {
    for (ValueTransformer transformer : valueTransformers) {
      value = transformer.apply(context, value);
    }

    return value;
  }

  InterceptorsExecutionResult before(ExecutionContext executionContext, List<Interceptor> interceptors) {

    List<Interceptor> interceptorList = new ArrayList<>(interceptors.size());

    try {
      for (Interceptor interceptor : interceptors) {
        interceptorList.add(interceptor);
        interceptor.before(executionContext);
      }
    } catch (Exception e) {
      return new InterceptorsExecutionResult(exceptionEnricherManager.handleThrowable(e), interceptorList);
    }
    return new InterceptorsExecutionResult(null, interceptorList);
  }

  private void onSuccess(ExecutionContext executionContext, Object result, List<Interceptor> interceptors) {
    intercept(interceptors, interceptor -> interceptor.onSuccess(executionContext, result),
              interceptor -> format(
                                    "Interceptor %s threw exception executing 'onSuccess' phase. Exception will be ignored. Next interceptors (if any) will be executed and the operation's result will be returned",
                                    interceptor));
  }

  private Throwable interceptError(ExecutionContext executionContext, Throwable e, List<Interceptor> interceptors) {
    Reference<Throwable> exceptionHolder = new Reference<>(e);

    intercept(interceptors, interceptor -> {
      Throwable decoratedException = interceptor.onError(executionContext, exceptionHolder.get());
      if (decoratedException != null) {
        exceptionHolder.set(decoratedException);
      }
    }, interceptor -> format(
                             "Interceptor %s threw exception executing 'interceptError' phase. Exception will be ignored. Next interceptors (if any) will be executed and the operation's exception will be returned",
                             interceptor));

    return exceptionHolder.get();
  }

  void after(ExecutionContext executionContext, Object result, List<Interceptor> interceptors) {
    intercept(interceptors, interceptor -> interceptor.after(executionContext, result),
              interceptor -> format(
                                    "Interceptor %s threw exception executing 'after' phase. Exception will be ignored. Next interceptors (if any) will be executed and the operation's result be returned",
                                    interceptor));
  }

  private void intercept(List<Interceptor> interceptors, Consumer<Interceptor> closure,
                         Function<Interceptor, String> exceptionMessageFunction) {
    if (!LOGGER.isDebugEnabled()) {
      interceptors.forEach(interceptor -> {
        try {
          closure.accept(interceptor);
        } catch (Exception e) {
          // Nothing to do
        }
      });
    } else {
      interceptors.forEach(interceptor -> {
        try {
          closure.accept(interceptor);
        } catch (Exception e) {
          LOGGER.debug(exceptionMessageFunction.apply(interceptor), e);
        }
      });
    }
  }

  private <T> ExecutionTemplate<T> getExecutionTemplate(ExecutionContextAdapter<ComponentModel> context) {
    return context.getTransactionConfig()
        .map(txConfig -> ((ExecutionTemplate<T>) createTransactionalExecutionTemplate(context.getMuleContext(), txConfig)))
        .orElse((ExecutionTemplate<T>) defaultExecutionTemplate);
  }

  private RetryPolicyTemplate getRetryPolicyTemplate(ExecutionContextAdapter<M> context) {
    // If there is a template, try to wrap it using the ReconnectionConfig
    Optional<RetryPolicyTemplate> customTemplate = context.getRetryPolicyTemplate()
        .map(delegate -> context.getConfiguration()
            .map(config -> config.getConnectionProvider().orElse(null))
            .map(provider -> connectionManager.getReconnectionConfigFor(provider).getRetryPolicyTemplate(delegate))
            .orElse(delegate));

    // In case of no template available in the context, use the one defined by the ConnectionProvider
    return customTemplate.orElseGet(() -> context.getConfiguration()
        .map(config -> config.getConnectionProvider().orElse(null))
        .map(provider -> connectionManager.getRetryTemplateFor((ConnectionProvider<? extends Object>) provider))
        .orElse(fallbackRetryPolicyTemplate));
  }

  private MutableConfigurationStats getMutableConfigurationStats(ExecutionContext<M> context) {
    return context.getConfiguration()
        .map(c -> (MutableConfigurationStats) c.getStatistics())
        .orElse(null);
  }

  private List<Interceptor> collectInterceptors(ExecutionContextAdapter<M> context, CompletableComponentExecutor<M> executor) {
    return collectInterceptors(context.getConfiguration(),
                               context instanceof PrecalculatedExecutionContextAdapter
                                   ? ((PrecalculatedExecutionContextAdapter) context).getOperationExecutor()
                                   : executor);
  }

  List<Interceptor> collectInterceptors(Optional<ConfigurationInstance> configurationInstance,
                                        CompletableComponentExecutor executor) {
    List<Interceptor> accumulator = new LinkedList<>();
    configurationInstance.ifPresent(config -> collectInterceptors(accumulator, config));
    collectInterceptors(accumulator, executor);

    return accumulator;
  }

  private void collectInterceptors(List<Interceptor> accumulator, Object subject) {
    if (subject instanceof Interceptable) {
      accumulator.addAll(((Interceptable) subject).getInterceptors());
    }
  }
}
