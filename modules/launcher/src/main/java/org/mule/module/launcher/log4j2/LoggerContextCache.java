/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Disposable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * A cache which relates {@link ClassLoader} instances
 * with {@link LoggerContext}s
 *
 *
 * Because the {@link LoggerContext} might contain
 * asynchronous loggers, this cache distinguises
 * between {@link #activeContexts} and
 * {@link #disposedContexts}.
 *
 * When a {@link LoggerContext} is removed
 * (either through {@link #remove(ClassLoader)}
 * or {@link #remove(LoggerContext)}), it is not
 * stopped right away. It is moved to th
 * {@link #disposedContexts} lists where it sits
 * during a lapse of {@link #disposeDelayInMillis}
 * before it is actually stopped. This is to the
 * asynchronous loggers some time to flush the
 * pending messages. Notice that there's no guarantee
 * that the waiting time is enough (although it should
 * for most cases). {@link #disposeDelayInMillis} defaults
 * to 15 seconds but it can be customized by setting the
 * {@link MuleProperties#MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS}
 * system property
 *
 * This class also implements the {@link Disposable}
 * interface. When {@link #dispose()} is invoked all
 * the contexts are stopped right away
 *
 * @since 3.7.0
 */
final class LoggerContextCache implements Disposable
{
    private static final long DEFAULT_DISPOSE_DELAY_IN_MILLIS = 15000;

    private final ArtifactAwareContextSelector artifactAwareContextSelector;
    private final Cache<Integer, LoggerContext> activeContexts;
    private final Cache<Integer, LoggerContext> disposedContexts;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private Long disposeDelayInMillis;

    LoggerContextCache(ArtifactAwareContextSelector artifactAwareContextSelector)
    {
        acquireContextDisposeDelay();

        this.artifactAwareContextSelector = artifactAwareContextSelector;
        activeContexts = CacheBuilder.newBuilder().build();

        disposedContexts = CacheBuilder.newBuilder()
                .expireAfterWrite(disposeDelayInMillis, TimeUnit.MILLISECONDS)
                .removalListener(new RemovalListener<Integer, LoggerContext>()
                {
                    @Override
                    public void onRemoval(RemovalNotification<Integer, LoggerContext> notification)
                    {
                        stop(notification.getValue());
                        activeContexts.invalidate(notification.getKey());
                    }
                })
                .build();
    }

    private void stop(LoggerContext loggerContext)
    {
        if (loggerContext != null && !loggerContext.isStopping() && !loggerContext.isStopped())
        {
            loggerContext.stop();
        }
    }

    private void acquireContextDisposeDelay()
    {
        try
        {
            disposeDelayInMillis = Long.valueOf(System.getProperty(MuleProperties.MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS));
        }
        catch (Exception e)
        {
            // value not set... ignore and use default
        }

        if (disposeDelayInMillis == null)
        {
            disposeDelayInMillis = DEFAULT_DISPOSE_DELAY_IN_MILLIS;
        }
    }

    LoggerContext getLoggerContext(final ClassLoader classLoader)
    {
        LoggerContext ctx;
        try
        {
            final int key = computeKey(classLoader);
            ctx = activeContexts.get(key, new Callable<LoggerContext>()
            {
                @Override
                public LoggerContext call() throws Exception
                {
                    LoggerContext context = disposedContexts.getIfPresent(key);
                    return context != null ? context : artifactAwareContextSelector.buildContext(classLoader);
                }
            });
        }
        catch (ExecutionException e)
        {
            throw new MuleRuntimeException(
                    createStaticMessage("Could not init logger context "), e);
        }

        if (ctx.getState() == LifeCycle.State.INITIALIZED)
        {
            ctx.start();
        }

        return ctx;
    }

    void remove(ClassLoader classLoader)
    {
        final Integer key = computeKey(classLoader);
        LoggerContext context = activeContexts.getIfPresent(key);
        if (context != null)
        {
            disposeContext(key, context);
        }
    }

    void remove(LoggerContext context)
    {
        for (Map.Entry<Integer, LoggerContext> entry : activeContexts.asMap().entrySet())
        {
            if (entry.getValue() == context)
            {
                disposeContext(entry.getKey(), context);
                return;
            }
        }
    }

    List<LoggerContext> getAllLoggerContexts()
    {
        return ImmutableList.copyOf(activeContexts.asMap().values());
    }

    private void disposeContext(Integer key, LoggerContext loggerContext)
    {
        disposedContexts.put(key, loggerContext);
        executorService.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                disposedContexts.cleanUp();
            }
        }, disposeDelayInMillis, TimeUnit.MILLISECONDS);
    }

    private int computeKey(ClassLoader classLoader)
    {
        return classLoader.hashCode();
    }

    @Override
    public void dispose()
    {
        executorService.shutdownNow();

        for (LoggerContext loggerContext : activeContexts.asMap().values())
        {
            stop(loggerContext);
        }

        activeContexts.invalidateAll();
        disposedContexts.invalidateAll();
        disposedContexts.cleanUp();
    }
}
