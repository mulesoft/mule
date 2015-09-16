/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static java.lang.String.format;
import org.mule.extension.introspection.Interceptable;
import org.mule.extension.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.runtime.ConfigurationStats;
import org.mule.extension.runtime.ConfigurationInstance;
import org.mule.extension.runtime.Interceptor;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.extension.runtime.RetryRequest;
import org.mule.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.util.ValueHolder;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExecutionMediator}.
 * <p/>
 * If the given {@code context} implements the {@link Interceptable}, then its defined
 * {@link Interceptor}s are properly executed as well.
 * <p/>
 * It also inspects the {@link ConfigurationStats} obtained from the {@link ConfigurationDeclaration}
 * in the {@code context}. If the stats class implements the {@link MutableConfigurationStats} interface,
 * then {@link MutableConfigurationStats#addInflightOperation()} and {@link MutableConfigurationStats#discountInflightOperation()}
 * are guaranteed to be called, whatever the operation's outcome.
 * <p/>
 * In case of operation failure, it will execute the {@link Interceptor#onError(OperationContext, RetryRequest, Exception)}
 * method of all the available interceptors, even if any of them request for a retry. When a retry request is granted,
 * the entire cycle of interception (before, onSuccess/onError, after) will be fired again, but no interceptor
 * which required a retry on the first execution will be allowed to request it again. If an interceptor makes such a requirement
 * after it already did on the first attempt, an {@link IllegalStateException} will be thrown. This is to prevent badly
 * written {@link Interceptor interceptors} from generating and endless loop by requesting the same retry over and over again.
 *
 * @since 4.0
 */
public final class DefaultExecutionMediator implements ExecutionMediator
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExecutionMediator.class);

    /**
     * Executes the operation per the specification in this classes' javadoc
     *
     * @param executor a {@link OperationExecutor}
     * @param context  the {@link OperationContext} for the {@code executor} to use
     * @return the operation's result
     * @throws Exception if the operation or a {@link Interceptor#before(OperationContext)} invokation fails
     */
    public Object execute(OperationExecutor executor, OperationContext context) throws Exception
    {
        final List<Interceptor> interceptors = collectInterceptors(context);
        final MutableConfigurationStats mutableStats = getMutableConfigurationStats(context);
        if (mutableStats != null)
        {
            mutableStats.addInflightOperation();
        }
        try
        {
            return executeWithInterceptors(executor, context, interceptors, new ValueHolder<>());
        }
        finally
        {
            if (mutableStats != null)
            {
                mutableStats.discountInflightOperation();
            }
        }
    }

    private MutableConfigurationStats getMutableConfigurationStats(OperationContext context)
    {
        ConfigurationStats stats = context.getConfiguration().getStatistics();
        return stats instanceof MutableConfigurationStats
               ? (MutableConfigurationStats) stats
               : null;
    }

    private Object executeWithInterceptors(OperationExecutor executor,
                                           OperationContext context,
                                           List<Interceptor> interceptors,
                                           ValueHolder<InterceptorsRetryRequest> retryRequestHolder) throws Exception
    {
        before(context, interceptors);
        Object result = null;
        Exception exception = null;
        try
        {
            result = executor.execute(context);
            onSuccess(context, result, interceptors);
        }
        catch (Exception e)
        {
            exception = onError(context, retryRequestHolder, e, interceptors);
        }
        finally
        {
            after(context, result, interceptors);
        }

        if (exception != null)
        {
            InterceptorsRetryRequest retryRequest = retryRequestHolder.get();
            if (retryRequest != null && retryRequest.isRetryRequested())
            {
                result = executeWithInterceptors(executor, context, interceptors, retryRequestHolder);
            }
            else
            {
                throw exception;
            }
        }

        return result;
    }

    private List<Interceptor> collectInterceptors(OperationContext operationContext)
    {
        ConfigurationInstance<?> configurationInstance = operationContext.getConfiguration();
        if (configurationInstance instanceof Interceptable)
        {
            return ((Interceptable) configurationInstance).getInterceptors();
        }

        return ImmutableList.of();
    }

    private void before(OperationContext operationContext, List<Interceptor> interceptors) throws Exception
    {
        for (Interceptor interceptor : interceptors)
        {
            interceptor.before(operationContext);
        }
    }

    private void onSuccess(OperationContext operationContext, Object result, List<Interceptor> interceptors)
    {
        intercept(interceptors,
                  (interceptor) -> interceptor.onSuccess(operationContext, result),
                  interceptor -> format("Interceptor %s threw exception executing 'onSuccess' phase. Exception will be ignored. Next interceptors (if any)" +
                                        "will be executed and the operation's result will be returned", interceptor));
    }

    private Exception onError(OperationContext operationContext,
                              ValueHolder<InterceptorsRetryRequest> retryRequestHolder,
                              Exception e,
                              List<Interceptor> interceptors)
    {
        ValueHolder<Exception> exceptionHolder = new ValueHolder<>(e);

        intercept(interceptors,
                  interceptor -> {
                      InterceptorsRetryRequest retryRequest = new InterceptorsRetryRequest(interceptor, retryRequestHolder.get());
                      retryRequestHolder.set(retryRequest);

                      Exception decoratedException = interceptor.onError(operationContext, retryRequest, exceptionHolder.get());
                      if (decoratedException != null)
                      {
                          exceptionHolder.set(decoratedException);
                      }
                  },
                  (interceptor) -> format("Interceptor %s threw exception executing 'onError' phase. Exception will be ignored. Next interceptors (if any)" +
                                          "will be executed and the operation's exception will be returned", interceptor));

        return exceptionHolder.get();
    }

    private void after(OperationContext operationContext, Object result, List<Interceptor> interceptors)
    {
        intercept(interceptors,
                  (interceptor) -> interceptor.after(operationContext, result),
                  (interceptor) -> format("Interceptor %s threw exception executing 'after' phase. Exception will be ignored. Next interceptors (if any)" +
                                          "will be executed and the operation's result be returned", interceptor));
    }

    private void intercept(List<Interceptor> interceptors, Consumer<Interceptor> closure, Function<Interceptor, String> exceptionMessageFunction)
    {
        interceptors.forEach(interceptor -> {
            try
            {
                closure.accept(interceptor);
            }
            catch (Exception e)
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(exceptionMessageFunction.apply(interceptor), e);
                }
            }
        });
    }

}
