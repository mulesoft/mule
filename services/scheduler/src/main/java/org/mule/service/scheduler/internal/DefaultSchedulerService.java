/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.service.scheduler.internal.config.ContainerThreadPoolsConfig.loadThreadPoolsConfig;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.scheduler.SchedulerPoolsConfigFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.scheduler.SchedulerContainerPoolsConfig;
import org.mule.service.scheduler.internal.threads.SchedulerThreadPools;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

/**
 * Default implementation of {@link SchedulerService}.
 *
 * @since 4.0
 */
public class DefaultSchedulerService implements SchedulerService, Startable, Stoppable {

  private static final Logger logger = getLogger(DefaultSchedulerService.class);

  private static final long DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 5000;

  private ReadWriteLock pollsLock = new ReentrantReadWriteLock();
  private Lock pollsReadLock = pollsLock.readLock();
  private Lock pollsWriteLock = pollsLock.writeLock();

  private int cores = getRuntime().availableProcessors();

  private LoadingCache<SchedulerPoolsConfigFactory, SchedulerThreadPools> poolsByConfig;
  private volatile boolean started = false;

  @Override
  public String getName() {
    return SchedulerService.class.getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    checkStarted();
    final SchedulerConfig config = config();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createCpuLightScheduler(config, 4 * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  public Scheduler ioScheduler() {
    checkStarted();
    final SchedulerConfig config = config();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createIoScheduler(config, cores * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  public Scheduler cpuIntensiveScheduler() {
    checkStarted();
    final SchedulerConfig config = config();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createCpuIntensiveScheduler(config, 4 * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createCpuLightScheduler(config, 4 * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  public Scheduler ioScheduler(SchedulerConfig config) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createIoScheduler(config, cores * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createCpuIntensiveScheduler(config, 4 * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  @Inject
  public Scheduler cpuLightScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config,
                                     SchedulerPoolsConfigFactory poolsConfigFactory) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(poolsConfigFactory)
          .createCpuLightScheduler(config, 4 * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  @Inject
  public Scheduler ioScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config,
                               SchedulerPoolsConfigFactory poolsConfigFactory) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(poolsConfigFactory)
          .createIoScheduler(config, cores * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  @Inject
  public Scheduler cpuIntensiveScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config,
                                         SchedulerPoolsConfigFactory poolsConfigFactory) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(poolsConfigFactory)
          .createCpuIntensiveScheduler(config, 4 * cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  @Inject
  public Scheduler customScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createCustomScheduler(config, cores, resolveStopTimeout(config));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  @Override
  @Inject
  public Scheduler customScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config, int queueSize) {
    checkStarted();
    pollsReadLock.lock();
    try {
      return poolsByConfig.get(SchedulerContainerPoolsConfig.getInstance())
          .createCustomScheduler(config, cores, resolveStopTimeout(config), queueSize);
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e.getCause());
    } finally {
      pollsReadLock.unlock();
    }
  }

  private Supplier<Long> resolveStopTimeout(SchedulerConfig config) {
    return () -> config.getShutdownTimeoutMillis().get() != null ? config.getShutdownTimeoutMillis().get()
        : DEFAULT_SHUTDOWN_TIMEOUT_MILLIS;
  }

  private void checkStarted() {
    if (!started) {
      throw new IllegalStateException("Service " + getName() + " is not started.");
    }
  }

  @Override
  public void start() throws MuleException {
    logger.info("Starting " + this.toString() + "...");

    pollsWriteLock.lock();
    try {
      poolsByConfig = newBuilder()
          .weakKeys()
          .removalListener(new RemovalListener<SchedulerPoolsConfigFactory, SchedulerThreadPools>() {

            @Override
            public void onRemoval(RemovalNotification<SchedulerPoolsConfigFactory, SchedulerThreadPools> notification) {
              try {
                notification.getValue().stop();
                logger.info("Stopped " + this.toString());
              } catch (InterruptedException e) {
                currentThread().interrupt();
                logger.info("Stop of " + this.toString() + " interrupted", e);
              } catch (MuleException e) {
                throw new MuleRuntimeException(e);
              }
            }
          })
          .build(new CacheLoader<SchedulerPoolsConfigFactory, SchedulerThreadPools>() {

            @Override
            public SchedulerThreadPools load(SchedulerPoolsConfigFactory key) throws Exception {
              SchedulerThreadPools containerThreadPools =
                  new SchedulerThreadPools(getName(), key.get().orElse(loadThreadPoolsConfig()));
              containerThreadPools.start();

              return containerThreadPools;
            }
          });

      logger.info("Started " + this.toString());
      started = true;
    } finally {
      pollsWriteLock.unlock();
    }
  }

  @Override
  public void stop() throws MuleException {
    logger.info("Stopping " + this.toString() + "...");
    pollsWriteLock.lock();
    try {
      started = false;

      poolsByConfig.invalidateAll();
      poolsByConfig = null;
    } finally {
      pollsWriteLock.unlock();
    }
  }

  @Override
  public List<Scheduler> getSchedulers() {
    List<Scheduler> schedulers = new ArrayList<>();

    for (SchedulerThreadPools schedulerThreadPools : poolsByConfig.asMap().values()) {
      schedulers.addAll(schedulerThreadPools.getSchedulers());
    }

    return unmodifiableList(schedulers);
  }
}
