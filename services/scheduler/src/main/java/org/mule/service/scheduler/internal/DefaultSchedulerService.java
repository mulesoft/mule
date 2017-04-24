/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.service.scheduler.internal.config.ContainerThreadPoolsConfig.loadThreadPoolsConfig;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.scheduler.SchedulerPoolsConfig;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.service.scheduler.internal.threads.SchedulerThreadPools;

import java.util.ArrayList;
import java.util.List;
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

  private int cores = getRuntime().availableProcessors();
  private SchedulerPoolsConfig containerThreadPoolsConfig;

  private SchedulerThreadPools containerThreadPools;

  private volatile boolean started = false;

  @Override
  public String getName() {
    return SchedulerService.class.getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    final SchedulerConfig config = config();
    return containerThreadPools.createCpuLightScheduler(config, 4 * cores, resolveStopTimeout(config));
  }

  @Override
  public Scheduler ioScheduler() {
    final SchedulerConfig config = config();
    return containerThreadPools.createIoScheduler(config, cores * cores, resolveStopTimeout(config));
  }

  @Override
  public Scheduler cpuIntensiveScheduler() {
    final SchedulerConfig config = config();
    return containerThreadPools.createCpuIntensiveScheduler(config, 4 * cores, resolveStopTimeout(config));
  }

  @Override
  @Inject
  public Scheduler cpuLightScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    return containerThreadPools.createCpuLightScheduler(config, 4 * cores, resolveStopTimeout(config));
  }

  @Override
  @Inject
  public Scheduler ioScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    return containerThreadPools.createIoScheduler(config, cores * cores, resolveStopTimeout(config));
  }

  @Override
  @Inject
  public Scheduler cpuIntensiveScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    return containerThreadPools.createCpuIntensiveScheduler(config, 4 * cores, resolveStopTimeout(config));
  }

  @Override
  @Inject
  public Scheduler customScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    checkStarted();
    return containerThreadPools.createCustomScheduler(config, cores, resolveStopTimeout(config));
  }

  @Override
  @Inject
  public Scheduler customScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config, int queueSize) {
    checkStarted();
    final Scheduler customScheduler =
        containerThreadPools.createCustomScheduler(config, cores, resolveStopTimeout(config), queueSize);
    return customScheduler;
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

    containerThreadPoolsConfig = loadThreadPoolsConfig();

    this.containerThreadPools = new SchedulerThreadPools(getName(), containerThreadPoolsConfig);
    this.containerThreadPools.start();

    logger.info("Started " + this.toString());
    started = true;
  }

  @Override
  public void stop() throws MuleException {
    started = false;
    logger.info("Stopping " + this.toString() + "...");

    try {
      this.containerThreadPools.stop();
      logger.info("Stopped " + this.toString());
    } catch (InterruptedException e) {
      currentThread().interrupt();
      logger.info("Stop of " + this.toString() + " interrupted", e);
    }

    this.containerThreadPools = null;
  }

  @Override
  public List<Scheduler> getSchedulers() {
    List<Scheduler> schedulers = new ArrayList<>();
    schedulers.addAll(containerThreadPools.getSchedulers());
    return unmodifiableList(schedulers);
  }
}
