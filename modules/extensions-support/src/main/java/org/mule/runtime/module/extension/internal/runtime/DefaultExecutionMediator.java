/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.core.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.core.work.SerialWorkManager;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.RetryRequest;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.exception.ExceptionEnricherManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
 * In case of operation failure, it will execute the {@link Interceptor#onError(ExecutionContext, RetryRequest, Throwable)} method
 * of all the available interceptors, even if any of them request for a retry. When a retry request is granted, the entire cycle
 * of interception (before, onSuccess/onError, after) will be fired again, but no interceptor which required a retry on the first
 * execution will be allowed to request it again. If an interceptor makes such a requirement after it already did on the first
 * attempt, an {@link IllegalStateException} will be thrown. This is to prevent badly written {@link Interceptor interceptors}
 * from generating and endless loop by requesting the same retry over and over again.
 *
 * @since 4.0
 */
public final class DefaultExecutionMediator implements ExecutionMediator {

  public static final SerialWorkManager WORK_MANAGER = new SerialWorkManager();
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExecutionMediator.class);
  private final ExceptionEnricherManager exceptionEnricherManager;
  private final ConnectionManagerAdapter connectionManager;
  private final ExecutionTemplate<?> defaultExecutionTemplate = callback -> callback.process();

  public DefaultExecutionMediator(ExtensionModel extensionModel, OperationModel operationModel,
                                  ConnectionManagerAdapter connectionManager) {
    this.connectionManager = connectionManager;
    this.exceptionEnricherManager = new ExceptionEnricherManager(extensionModel, operationModel);
  }

  /**
   * Executes the operation per the specification in this classes' javadoc
   *
   * @param executor an {@link OperationExecutor}
   * @param context the {@link ExecutionContextAdapter} for the {@code executor} to use
   * @return the operation's result
   * @throws Exception if the operation or a {@link Interceptor#before(ExecutionContext)} invokation fails
   */
  public Object execute(OperationExecutor executor, ExecutionContextAdapter context) throws Throwable {
    final List<Interceptor> interceptors = collectInterceptors(context.getConfiguration(), executor);
    final MutableConfigurationStats mutableStats = getMutableConfigurationStats(context);
    if (mutableStats != null) {
      mutableStats.addInflightOperation();
    }
    try {
      return executeWithRetryPolicy(executor, context, interceptors);
    } finally {
      if (mutableStats != null) {
        mutableStats.discountInflightOperation();
      }
    }
  }

  private Object executeWithRetryPolicy(OperationExecutor executor, ExecutionContextAdapter context,
                                        List<Interceptor> interceptors)
      throws Throwable {
    RetryPolicyTemplate retryPolicyTemplate = getRetryPolicyTemplate(context.getConfiguration());

    ExecutionTemplate<RetryContext> executionTemplate = getExecutionTemplate(context);

    final OperationRetryCallBack connectionRetry = new OperationRetryCallBack(executor, context, interceptors);
    // TODO - MULE-9336 - Add support for non blocking retry policies
    // TODO - MULE-10579 - Reconnection should be centralized
    executionTemplate.execute(() -> retryPolicyTemplate.execute(connectionRetry, WORK_MANAGER));
    Throwable exception = connectionRetry.getOperationExecutionResult().getException();
    if (exception != null) {
      throw exception;
    }
    return connectionRetry.getOperationExecutionResult().getOutput();
  }

  private OperationExecutionResult executeWithInterceptors(OperationExecutor executor, ExecutionContextAdapter context,
                                                           List<Interceptor> interceptors,
                                                           ValueHolder<InterceptorsRetryRequest> retryRequestHolder) {
    Object result = null;
    Throwable exception = null;


    InterceptorsExecutionResult beforeExecutionResult = before(context, interceptors);

    try {
      if (beforeExecutionResult.isOk()) {
        result = executor.execute(context);
        onSuccess(context, result, interceptors);
      } else {
        interceptors = beforeExecutionResult.getExecutedInterceptors();
        throw beforeExecutionResult.getThrowable();
      }
    } catch (Throwable e) {
      exception = exceptionEnricherManager.processException(e);
      exception = onError(context, retryRequestHolder, exception, interceptors);
    } finally {
      after(context, result, interceptors);
    }

    return new OperationExecutionResult(result, exception, Optional.ofNullable(retryRequestHolder.get()));
  }


  private InterceptorsExecutionResult before(ExecutionContext executionContext, List<Interceptor> interceptors) {

    List<Interceptor> interceptorList = new ArrayList<>();

    try {
      for (Interceptor interceptor : interceptors) {
        interceptorList.add(interceptor);
        interceptor.before(executionContext);
      }
    } catch (Exception e) {
      return new InterceptorsExecutionResult(exceptionEnricherManager.handleException(e), interceptorList);
    }
    return new InterceptorsExecutionResult(null, interceptorList);
  }

  private void onSuccess(ExecutionContext executionContext, Object result, List<Interceptor> interceptors) {
    intercept(interceptors,
              interceptor -> interceptor.onSuccess(executionContext, result), interceptor -> format(
                                                                                                    "Interceptor %s threw exception executing 'onSuccess' phase. Exception will be ignored. Next interceptors (if any)"
                                                                                                        + "will be executed and the operation's result will be returned",
                                                                                                    interceptor));
  }

  private Throwable onError(ExecutionContext executionContext, ValueHolder<InterceptorsRetryRequest> retryRequestHolder,
                            Throwable e, List<Interceptor> interceptors) {
    ValueHolder<Throwable> exceptionHolder = new ValueHolder<>(e);

    intercept(interceptors, interceptor -> {
      InterceptorsRetryRequest retryRequest = new InterceptorsRetryRequest(interceptor, retryRequestHolder.get());
      retryRequestHolder.set(retryRequest);

      Throwable decoratedException = interceptor.onError(executionContext, retryRequest, exceptionHolder.get());
      if (decoratedException != null) {
        exceptionHolder.set(decoratedException);
      }
    }, interceptor -> format("Interceptor %s threw exception executing 'onError' phase. Exception will be ignored. Next interceptors (if any)"
        + "will be executed and the operation's exception will be returned", interceptor));

    return exceptionHolder.get();
  }

  private void after(ExecutionContext executionContext, Object result, List<Interceptor> interceptors) {
    {
      intercept(interceptors,
                interceptor -> interceptor.after(executionContext, result), interceptor -> format(
                                                                                                  "Interceptor %s threw exception executing 'after' phase. Exception will be ignored. Next interceptors (if any)"
                                                                                                      + "will be executed and the operation's result be returned",
                                                                                                  interceptor));
    }
  }

  private void intercept(List<Interceptor> interceptors, Consumer<Interceptor> closure,
                         Function<Interceptor, String> exceptionMessageFunction) {
    interceptors.forEach(interceptor -> {
      try {
        closure.accept(interceptor);
      } catch (Exception e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(exceptionMessageFunction.apply(interceptor), e);
        }
      }
    });
  }

  private <T> ExecutionTemplate<T> getExecutionTemplate(ExecutionContextAdapter<OperationModel> context) {
    return context.getTransactionConfig()
        .map(txConfig -> ((ExecutionTemplate<T>) createTransactionalExecutionTemplate(context.getMuleContext(), txConfig)))
        .orElse((ExecutionTemplate<T>) defaultExecutionTemplate);
  }

  //TODO: MULE-10580 - Operation reconnection should be decoupled from config reconnection
  private RetryPolicyTemplate getRetryPolicyTemplate(Optional<ConfigurationInstance> configurationInstance) {
    Optional<ConnectionProvider> connectionProviderOptional = configurationInstance.map(
                                                                                        ConfigurationInstance::getConnectionProvider)
        .orElse(empty());

    if (connectionProviderOptional.isPresent()) {
      final ConnectionProvider connectionProvider = connectionProviderOptional.get();
      if (ConnectionProviderWrapper.class.isAssignableFrom(connectionProvider.getClass())) {
        return ((ConnectionProviderWrapper) connectionProvider).getRetryPolicyTemplate();
      }
    }
    return connectionManager.getDefaultRetryPolicyTemplate();
  }

  private MutableConfigurationStats getMutableConfigurationStats(ExecutionContext<ComponentModel> context) {
    ConfigurationStats stats = context.getConfiguration().map(ConfigurationInstance::getStatistics).orElse(null);
    return stats instanceof MutableConfigurationStats ? (MutableConfigurationStats) stats : null;
  }

  private List<Interceptor> collectInterceptors(Optional<ConfigurationInstance> configurationInstance,
                                                OperationExecutor executor) {
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

  private class OperationRetryCallBack implements RetryCallback {

    private final ExecutionContextAdapter<OperationModel> context;
    private final List<Interceptor> interceptorList;
    private OperationExecutor operationExecutor;
    private OperationExecutionResult operationExecutionResult;

    private OperationRetryCallBack(OperationExecutor operationExecutor, ExecutionContextAdapter context,
                                   List<Interceptor> interceptorList) {
      this.operationExecutor = operationExecutor;
      this.context = context;
      this.interceptorList = interceptorList;
    }

    @Override
    public void doWork(RetryContext retryContext) throws Exception {
      operationExecutionResult = (OperationExecutionResult) getExecutionTemplate(context)
          .execute(() -> executeWithInterceptors(operationExecutor, context, interceptorList, new ValueHolder<>()));

      if (!operationExecutionResult.isOk()) {
        if (operationExecutionResult.getRetryRequest().isPresent()
            && operationExecutionResult.getRetryRequest().get().isRetryRequested()) {
          Throwable throwable = operationExecutionResult.getException();
          if (throwable instanceof Exception) {
            throw (Exception) throwable;
          } else {
            throw new MuleRuntimeException(throwable);
          }
        } else {
          retryContext.setFailed(operationExecutionResult.getException());
        }
      }
    }

    @Override
    public String getWorkDescription() {
      StringBuilder description = new StringBuilder(format("Extension '%s'", context.getExtensionModel().getName()));
      context.getConfiguration().ifPresent(conf -> description.append(format(" with configuration '%s'", conf.getName())));

      return description.toString();
    }

    @Override
    public Object getWorkOwner() {
      return this;
    }

    public OperationExecutionResult getOperationExecutionResult() {
      return operationExecutionResult;
    }
  }
}
