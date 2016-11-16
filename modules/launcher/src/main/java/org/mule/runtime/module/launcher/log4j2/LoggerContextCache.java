/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.api.lifecycle.Disposable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * A cache which relates {@link ClassLoader} instances with {@link LoggerContext}s
 *
 *
 * Because the {@link LoggerContext} might contain asynchronous loggers, this cache distinguises between {@link #activeContexts}
 * and {@link #disposedContexts}.
 *
 * When a {@link LoggerContext} is removed (either through {@link #remove(ClassLoader)} or {@link #remove(LoggerContext)}), it is
 * not stopped right away. It is moved to the {@link #disposedContexts} list where it sits during a lapse of
 * {@link #disposeDelayInMillis} before it is actually stopped. This is to give asynchronous loggers some time to flush the
 * pending messages. Notice that there's no guarantee that the waiting time is enough (although it should for most cases).
 * {@link #disposeDelayInMillis} defaults to 15 seconds but it can be customized by setting the
 * {@link MuleProperties#MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS} system property
 *
 * This class also implements the {@link Disposable} interface. When {@link #dispose()} is invoked all the contexts are stopped
 * right away
 *
 * @since 3.7.0
 */
final class LoggerContextCache implements Disposable {

  private static final long DEFAULT_DISPOSE_DELAY_IN_MILLIS = 15000;

  private final ArtifactAwareContextSelector artifactAwareContextSelector;
  // Extra cache layer to avid some nasty implications for using guava cache at this point. See the comments in
  // #doGetLoggerContext(final ClassLoader classLoader) for details.
  private final Map<Integer, LoggerContext> builtContexts = new ConcurrentHashMap<>();
  private final Cache<Integer, LoggerContext> activeContexts;
  private final Cache<Integer, LoggerContext> disposedContexts;
  private final ScheduledExecutorService executorService;
  private Long disposeDelayInMillis;

  LoggerContextCache(ArtifactAwareContextSelector artifactAwareContextSelector, ClassLoader reaperContextClassLoader) {
    acquireContextDisposeDelay();
    this.artifactAwareContextSelector = artifactAwareContextSelector;
    activeContexts = CacheBuilder.newBuilder().build();

    disposedContexts = CacheBuilder.newBuilder().expireAfterWrite(disposeDelayInMillis, TimeUnit.MILLISECONDS)
        .removalListener(new RemovalListener<Integer, LoggerContext>() {

          @Override
          public void onRemoval(RemovalNotification<Integer, LoggerContext> notification) {
            stop(notification.getValue());
            activeContexts.invalidate(notification.getKey());
            builtContexts.remove(notification.getKey());
          }
        }).build();

    executorService = newScheduledThreadPool(1, new LoggerContextReaperThreadFactory(reaperContextClassLoader));
  }

  private void stop(LoggerContext loggerContext) {
    if (loggerContext != null && !loggerContext.isStopping() && !loggerContext.isStopped()) {
      loggerContext.stop();
    }
  }

  private void acquireContextDisposeDelay() {
    try {
      disposeDelayInMillis = Long.valueOf(System.getProperty(MuleProperties.MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS));
    } catch (Exception e) {
      // value not set... ignore and use default
    }

    if (disposeDelayInMillis == null) {
      disposeDelayInMillis = DEFAULT_DISPOSE_DELAY_IN_MILLIS;
    }
  }

  LoggerContext getLoggerContext(final ClassLoader classLoader) {
    final LoggerContext ctx;
    try {
      final Integer key = computeKey(classLoader);
      // If possible, avoid using guava cache since the callable puts unwanted pressure on the garbage collector.
      if (builtContexts.containsKey(key)) {
        ctx = builtContexts.get(key);
      } else {
        synchronized (this) {
          if (builtContexts.containsKey(key)) {
            ctx = builtContexts.get(key);
          } else {
            ctx = doGetLoggerContext(classLoader, key);
          }
        }
      }
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not init logger context "), e);
    }

    if (ctx.getState() == LifeCycle.State.INITIALIZED) {
      synchronized (this) {
        if (ctx.getState() == LifeCycle.State.INITIALIZED) {
          ctx.start();
        }
      }
    }

    return ctx;
  }

  /**
   * The {@link Callable} passed to the guasa cache, because
   * 
   * Guava cache will use its logging framework to log something, and that logger will end up calling here.
   * <p>
   * With the check in the {@link Callable} passed to the guava cache, we avoid building an extra context. We cannot just use a
   * map, because it may result in an eternal recurrent call, guava does a good job at handling that situation. It is just the
   * logging that guava tries to do that may disrupt thing when initializing the logging infrastructure.
   * 
   * @param classLoader
   * @param key
   * @return
   * @throws ExecutionException
   */
  protected LoggerContext doGetLoggerContext(final ClassLoader classLoader, final Integer key) throws ExecutionException {
    return activeContexts.get(key, new Callable<LoggerContext>() {

      @Override
      public LoggerContext call() throws Exception {
        if (builtContexts.containsKey(key)) {
          return builtContexts.get(key);
        } else {
          LoggerContext context = artifactAwareContextSelector.buildContext(classLoader);
          builtContexts.put(key, context);
          return context;
        }
      }
    });
  }

  void remove(ClassLoader classLoader) {
    final Integer key = computeKey(classLoader);
    LoggerContext context = activeContexts.getIfPresent(key);
    if (context != null) {
      disposeContext(key, context);
    }
  }

  void remove(LoggerContext context) {
    for (Map.Entry<Integer, LoggerContext> entry : activeContexts.asMap().entrySet()) {
      if (entry.getValue() == context) {
        disposeContext(entry.getKey(), context);
        return;
      }
    }
  }

  List<LoggerContext> getAllLoggerContexts() {
    return ImmutableList.copyOf(activeContexts.asMap().values());
  }

  private void disposeContext(Integer key, LoggerContext loggerContext) {
    if (isDisposedClassLoader(key)) {
      return;
    }

    disposedContexts.put(key, loggerContext);
    synchronized (executorService) {
      if (!executorService.isShutdown()) {
        executorService.schedule(new Runnable() {

          @Override
          public void run() {
            disposedContexts.cleanUp();
          }
        }, disposeDelayInMillis + 1, TimeUnit.MILLISECONDS); // add one millisecond to make sure entries will be
                                                             // expired
      }
    }
  }

  private int computeKey(ClassLoader classLoader) {
    return classLoader.hashCode();
  }

  private boolean isDisposedClassLoader(int classLoaderHashCode) {
    return disposedContexts.asMap().containsKey(classLoaderHashCode);
  }

  @Override
  public void dispose() {
    synchronized (executorService) {
      executorService.shutdownNow();
    }

    for (LoggerContext loggerContext : activeContexts.asMap().values()) {
      stop(loggerContext);
    }

    activeContexts.invalidateAll();
    builtContexts.clear();
    disposedContexts.invalidateAll();
    disposedContexts.cleanUp();
  }
}
