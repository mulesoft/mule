/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.lang.Boolean.parseBoolean;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED;
import static org.mule.runtime.module.extension.internal.runtime.client.strategy.OperationMessageProcessorUtils.disposeProcessor;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ForwardingExecutorService;

/**
 * Class that provides a {@link ExtensionsClientProcessorsStrategy} according to the value of the system property
 * MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED defined in {@link org.mule.runtime.api.util.MuleSystemProperties}
 *
 * @since 4.1.6
 */
public class ExtensionsClientProcessorsStrategyFactory implements Initialisable, Disposable {

  private static int MAX_CACHE_SIZE = 100;
  private static int CACHE_ENTRY_EXPIRE_AFTER_ACCESS_IN_MINUTES = 10;

  private boolean usesCachedStrategy;
  private Cache<String, OperationMessageProcessor> operationMessageProcessorCache;
  private final ShutdownExecutor cacheShutdownExecutor = new ShutdownExecutor();

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private Registry registry;

  @Inject
  private MuleContext muleContext;

  @Inject
  private PolicyManager policyManager;

  @Inject
  private ReflectionCache reflectionCache;

  public ExtensionsClientProcessorsStrategyFactory() {

  }

  /**
   * This method return the suitable {@link ExtensionsClientProcessorsStrategy}
   */
  public ExtensionsClientProcessorsStrategy create(CoreEvent event) {
    return usesCachedStrategy
        ? new CachedExtensionsClientProcessorsStrategy(extensionManager, registry, muleContext, policyManager, reflectionCache,
                                                       event, operationMessageProcessorCache)
        : new NonCachedExtensionsClientProcessorsStrategy(extensionManager, registry, muleContext, policyManager, reflectionCache,
                                                          event);
  }

  @Override
  public void initialise() throws InitialisationException {
    usesCachedStrategy = !parseBoolean(System.getProperty(MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED));
    if (usesCachedStrategy) {
      operationMessageProcessorCache = createCache();
    }
  }

  private Cache<String, OperationMessageProcessor> createCache() {
    return Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        // Since the removal listener runs asynchronously, force waiting for all cleanup tasks to be complete before proceeding
        // (and finalizing) the context disposal.
        // Ref: https://github.com/ben-manes/caffeine/issues/104#issuecomment-238068997
        .executor(cacheShutdownExecutor)
        .expireAfterAccess(CACHE_ENTRY_EXPIRE_AFTER_ACCESS_IN_MINUTES, MINUTES)
        .<String, OperationMessageProcessor>removalListener((key, operationMessageProcessor,
                                                             removalCause) -> disposeProcessor(operationMessageProcessor))
        .build();
  }

  @Override
  public void dispose() {
    if (operationMessageProcessorCache != null) {
      operationMessageProcessorCache.invalidateAll();
    }
    cacheShutdownExecutor.shutdown();
    shutdownAndAwaitTermination(cacheShutdownExecutor, 5, SECONDS);
  }

  private static final class ShutdownExecutor extends ForwardingExecutorService {

    private final AtomicInteger tasks = new AtomicInteger();
    private final Semaphore semaphore = new Semaphore(0);
    private volatile boolean shutdown;

    @Override
    public void execute(Runnable task) {
      if (shutdown) {
        throw new RejectedExecutionException("Shutdown");
      }

      tasks.incrementAndGet();
      delegate().execute(() -> {
        try {
          task.run();
        } finally {
          semaphore.release();
        }
      });
    }

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      int permits = tasks.get();
      boolean terminated = semaphore.tryAcquire(permits, timeout, unit);
      if (terminated) {
        semaphore.release(permits);
      }
      return terminated && shutdown;
    }

    @Override
    public boolean isTerminated() {
      try {
        return awaitTermination(0, SECONDS);
      } catch (InterruptedException e) {
        return false;
      }
    }

    @Override
    protected ExecutorService delegate() {
      return commonPool();
    }
  }

}
