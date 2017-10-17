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
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;

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
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.exception.ExceptionHandlerManager;
import org.mule.runtime.module.extension.internal.runtime.exception.ModuleExceptionHandler;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

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
 * onSuccess/onError, after) will be fired again.
 *
 * @since 4.0
 */
public final class DefaultExecutionMediator<T extends ComponentModel> implements ExecutionMediator<T> {

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
                                  T operationModel,
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
   * @param executor an {@link ComponentExecutor}
   * @param context the {@link ExecutionContextAdapter} for the {@code executor} to use
   * @return the operation's result
   * @throws Exception if the operation or a {@link Interceptor#before(ExecutionContext)} invokation fails
   */
  @Override
  public Publisher<Object> execute(ComponentExecutor<T> executor, ExecutionContextAdapter<T> context) {
    final Optional<MutableConfigurationStats> stats = getMutableConfigurationStats(context);
    stats.ifPresent(s -> s.addInflightOperation());

    try {
      return (Mono<Object>) getExecutionTemplate((ExecutionContextAdapter<ComponentModel>) context)
          .execute(() -> executeWithInterceptors(executor, context, collectInterceptors(context, executor), stats));
    } catch (Exception e) {
      return error(e);
    } catch (Throwable t) {
      return error(wrapFatal(t));
    }
  }

  private Mono<Object> executeWithInterceptors(ComponentExecutor<T> executor,
                                               ExecutionContextAdapter<T> context,
                                               final List<Interceptor> interceptors,
                                               Optional<MutableConfigurationStats> stats) {

    List<Interceptor> executedInterceptors = new ArrayList<>(interceptors.size());
    // If the operation is retried, then the interceptors need to be executed again,
    // so we wrap the mono which executes the operation into another which sets up
    // the context and is the one configured with the retry logic
    Mono<Object> publisher = Mono.create(sink -> {
      Mono<Object> result;

      InterceptorsExecutionResult beforeExecutionResult = before(context, interceptors);
      if (beforeExecutionResult.isOk()) {
        result = from(withContextClassLoader(getClassLoader(context.getExtensionModel()), () -> executor.execute(context)));
        executedInterceptors.addAll(interceptors);
      } else {
        result = error(beforeExecutionResult.getThrowable());
        executedInterceptors.addAll(beforeExecutionResult.getExecutedInterceptors());
      }

      result
          .map(value -> transform(context, value))
          .doOnSuccess(value -> {
            onSuccess(context, value, interceptors);
            stats.ifPresent(s -> s.discountInflightOperation());
            sink.success(value);
          }).onErrorMap(t -> mapError(context, interceptors, t))
          .subscribe(v -> {
          }, sink::error);
    })
        .doOnSuccessOrError((value, e) -> {
          try {
            after(context, value, executedInterceptors);
          } finally {
            executedInterceptors.clear();
          }
        });

    return from(getRetryPolicyTemplate(context).applyPolicy(publisher,
                                                            e -> extractConnectionException(e).isPresent(),
                                                            e -> stats.ifPresent(s -> s.discountInflightOperation()),
                                                            throwable -> throwable));
  }

  private Throwable mapError(ExecutionContextAdapter context, List<Interceptor> interceptors, Throwable e) {
    e = exceptionEnricherManager.process(e);
    e = moduleExceptionHandler.processException(e);
    e = onError(context, e, interceptors);

    return e;
  }

  private Object transform(ExecutionContextAdapter context, Object value) {
    for (ValueTransformer transformer : valueTransformers) {
      value = transformer.apply(context, value);
    }

    return value;
  }

  InterceptorsExecutionResult before(ExecutionContext executionContext, List<Interceptor> interceptors) {

    List<Interceptor> interceptorList = new ArrayList<>();

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

  private Throwable onError(ExecutionContext executionContext, Throwable e, List<Interceptor> interceptors) {
    Reference<Throwable> exceptionHolder = new Reference<>(e);

    intercept(interceptors, interceptor -> {
      Throwable decoratedException = interceptor.onError(executionContext, exceptionHolder.get());
      if (decoratedException != null) {
        exceptionHolder.set(decoratedException);
      }
    }, interceptor -> format(
                             "Interceptor %s threw exception executing 'onError' phase. Exception will be ignored. Next interceptors (if any) will be executed and the operation's exception will be returned",
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

  private <T> ExecutionTemplate<T> getExecutionTemplate(ExecutionContextAdapter<ComponentModel> context) {
    return context.getTransactionConfig()
        .map(txConfig -> ((ExecutionTemplate<T>) createTransactionalExecutionTemplate(context.getMuleContext(), txConfig)))
        .orElse((ExecutionTemplate<T>) defaultExecutionTemplate);
  }

  private RetryPolicyTemplate getRetryPolicyTemplate(ExecutionContextAdapter<T> context) {
    return context.getRetryPolicyTemplate().orElseGet(() -> context.getConfiguration()
        .flatMap(ConfigurationInstance::getConnectionProvider)
        .map(provider -> {
          if (provider instanceof ConnectionProviderWrapper) {
            return ((ConnectionProviderWrapper) provider).getRetryPolicyTemplate();
          }

          return connectionManager.getRetryTemplateFor((ConnectionProvider<? extends Object>) provider);
        }).orElse(fallbackRetryPolicyTemplate));
  }

  private Optional<MutableConfigurationStats> getMutableConfigurationStats(ExecutionContext<T> context) {
    return context.getConfiguration()
        .map(ConfigurationInstance::getStatistics)
        .filter(s -> s instanceof MutableConfigurationStats)
        .map(s -> (MutableConfigurationStats) s);
  }

  private List<Interceptor> collectInterceptors(ExecutionContextAdapter<T> context, ComponentExecutor<T> executor) {
    return collectInterceptors(context.getConfiguration(),
                               context instanceof PrecalculatedExecutionContextAdapter
                                   ? ((PrecalculatedExecutionContextAdapter) context).getOperationExecutor()
                                   : executor);
  }

  List<Interceptor> collectInterceptors(Optional<ConfigurationInstance> configurationInstance,
                                        ComponentExecutor executor) {
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
