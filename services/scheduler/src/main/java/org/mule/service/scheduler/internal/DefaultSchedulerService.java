/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.service.scheduler.ThreadType.CPU_INTENSIVE;
import static org.mule.service.scheduler.ThreadType.CPU_LIGHT;
import static org.mule.service.scheduler.ThreadType.CUSTOM;
import static org.mule.service.scheduler.ThreadType.IO;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.loadThreadPoolsConfig;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.service.scheduler.ThreadType;
import org.mule.service.scheduler.internal.config.ThreadPoolsConfig;
import org.mule.service.scheduler.internal.threads.SchedulerThreadFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;

/**
 * Default implementation of {@link SchedulerService}.
 * <p>
 * {@link Scheduler}s provided by this implementation of {@link SchedulerService} use a shared single-threaded
 * {@link ScheduledExecutorService} for scheduling work. When a scheduled tasks is fired, they are executed using the
 * {@link Scheduler}'s own executor.
 * <p>
 * The returned {@link Scheduler}s have an {@code AbortPolicy} rejection policy. That means that when sending a task to a full
 * {@link Scheduler} a {@link RejectedExecutionException} will be thrown.
 *
 * @since 4.0
 */
public class DefaultSchedulerService implements SchedulerService, Startable, Stoppable {

  private static final Logger logger = getLogger(DefaultSchedulerService.class);

  private static final String CPU_LIGHT_THREADS_NAME = SchedulerService.class.getSimpleName() + "_" + CPU_LIGHT.getName();
  private static final String IO_THREADS_NAME = SchedulerService.class.getSimpleName() + "_" + IO.getName();
  private static final String COMPUTATION_THREADS_NAME = SchedulerService.class.getSimpleName() + "_" + CPU_INTENSIVE.getName();
  private static final String TIMER_THREADS_NAME = SchedulerService.class.getSimpleName() + "_timer";
  private static final String CUSTOM_THREADS_NAME = SchedulerService.class.getSimpleName() + "_" + CUSTOM.getName();

  private int cores = getRuntime().availableProcessors();
  private ThreadPoolsConfig threadPoolsConfig;

  private final ThreadGroup schedulerGroup = new ThreadGroup(getName());
  private final ThreadGroup cpuLightGroup = new ThreadGroup(schedulerGroup, CPU_LIGHT_THREADS_NAME);
  private final ThreadGroup ioGroup = new ThreadGroup(schedulerGroup, IO_THREADS_NAME);
  private final ThreadGroup computationGroup = new ThreadGroup(schedulerGroup, COMPUTATION_THREADS_NAME);
  private final ThreadGroup timerGroup = new ThreadGroup(schedulerGroup, TIMER_THREADS_NAME);
  private final ThreadGroup customGroup = new ThreadGroup(schedulerGroup, CUSTOM_THREADS_NAME);

  private ExecutorService cpuLightExecutor;
  private ExecutorService ioExecutor;
  private ExecutorService computationExecutor;
  private Set<ExecutorService> customSchedulersExecutors = new HashSet<>();
  private ScheduledThreadPoolExecutor scheduledExecutor;
  private org.quartz.Scheduler quartzScheduler;

  @Override
  public String getName() {
    return "SchedulerService";
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return new DefaultScheduler(resolveSchedulerCreationLocation(CPU_LIGHT_THREADS_NAME), cpuLightExecutor, 4 * cores,
                                scheduledExecutor, quartzScheduler, CPU_LIGHT);
  }

  @Override
  public Scheduler ioScheduler() {
    return new DefaultScheduler(resolveSchedulerCreationLocation(IO_THREADS_NAME), ioExecutor, cores * cores,
                                scheduledExecutor, quartzScheduler, IO);
  }

  @Override
  public Scheduler cpuIntensiveScheduler() {
    return new DefaultScheduler(resolveSchedulerCreationLocation(COMPUTATION_THREADS_NAME), computationExecutor, 4 * cores,
                                scheduledExecutor, quartzScheduler, CPU_INTENSIVE);
  }

  @Override
  public Scheduler customScheduler(String name, int corePoolSize) {
    final ExecutorService executor =
        new ThreadPoolExecutor(corePoolSize, corePoolSize, 0L, MILLISECONDS, new SynchronousQueue<Runnable>(),
                               new SchedulerThreadFactory(customGroup, "%s." + name + ".%02d"));
    final DefaultScheduler customScheduler = new CustomScheduler(resolveSchedulerCreationLocation(name), executor, cores,
                                                                 scheduledExecutor, quartzScheduler, CUSTOM);
    return customScheduler;
  }

  @Override
  public Scheduler customScheduler(String name, int corePoolSize, int queueSize) {
    final ExecutorService executor =
        new ThreadPoolExecutor(corePoolSize, corePoolSize, 0L, MILLISECONDS, new LinkedBlockingQueue<Runnable>(queueSize),
                               new SchedulerThreadFactory(customGroup, "%s." + name + ".%02d"));
    final DefaultScheduler customScheduler = new CustomScheduler(resolveSchedulerCreationLocation(name), executor, cores,
                                                                 scheduledExecutor, quartzScheduler, CUSTOM);
    customSchedulersExecutors.add(customScheduler);
    return customScheduler;
  }

  private class CustomScheduler extends DefaultScheduler {

    private final ExecutorService executor;

    private CustomScheduler(String name, ExecutorService executor, int workers, ScheduledExecutorService scheduledExecutor,
                            org.quartz.Scheduler quartzScheduler, ThreadType threadsType) {
      super(name, executor, workers, scheduledExecutor, quartzScheduler, threadsType);
      this.executor = executor;
    }

    @Override
    public void shutdown() {
      super.shutdown();
      executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
      final List<Runnable> cancelledTasks = super.shutdownNow();
      executor.shutdownNow();
      customSchedulersExecutors.remove(this);
      return cancelledTasks;
    }
  }

  private String resolveSchedulerCreationLocation(String prefix) {
    int i = 0;
    final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    StackTraceElement ste = stackTrace[i++];
    // We have to go deep enough, right before the proxy call
    while (!ste.getClassName().contains("$Proxy") && i < stackTrace.length) {
      ste = stackTrace[i++];
    }
    if (ste.getClassName().contains("$Proxy")) {
      ste = stackTrace[i++];
    } else {
      ste = stackTrace[2];
    }

    return prefix + "@" + (ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber());
  }

  @Override
  public void start() throws MuleException {
    logger.info("Starting " + this.toString() + "...");

    threadPoolsConfig = loadThreadPoolsConfig();

    cpuLightExecutor = new ThreadPoolExecutor(threadPoolsConfig.getCpuLightPoolSize(), threadPoolsConfig.getCpuLightPoolSize(),
                                              0, SECONDS, new LinkedBlockingQueue<>(), new SchedulerThreadFactory(cpuLightGroup));
    ioExecutor = new ThreadPoolExecutor(threadPoolsConfig.getIoCorePoolSize(), threadPoolsConfig.getIoMaxPoolSize(),
                                        threadPoolsConfig.getIoKeepAlive(), MILLISECONDS, new SynchronousQueue<>(),
                                        new SchedulerThreadFactory(ioGroup));
    computationExecutor =
        new ThreadPoolExecutor(threadPoolsConfig.getCpuIntensivePoolSize(), threadPoolsConfig.getCpuIntensivePoolSize(),
                               0, SECONDS, new LinkedBlockingQueue<>(),
                               new SchedulerThreadFactory(computationGroup));
    scheduledExecutor = new ScheduledThreadPoolExecutor(1, new SchedulerThreadFactory(timerGroup, "%s"));
    scheduledExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    scheduledExecutor.setRemoveOnCancelPolicy(true);
    StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
    try {
      schedulerFactory.initialize(threadPoolsConfig.defaultQuartzProperties(getName()));
      quartzScheduler = schedulerFactory.getScheduler();
      quartzScheduler.start();
    } catch (SchedulerException e) {
      throw new LifecycleException(e, this);
    }

    ((ThreadPoolExecutor) cpuLightExecutor).prestartAllCoreThreads();
    ((ThreadPoolExecutor) ioExecutor).prestartAllCoreThreads();
    ((ThreadPoolExecutor) computationExecutor).prestartAllCoreThreads();
    ((ThreadPoolExecutor) scheduledExecutor).prestartAllCoreThreads();

    logger.info("Started " + this.toString());
  }

  @Override
  public void stop() throws MuleException {
    logger.info("Stopping " + this.toString() + "...");

    cpuLightExecutor.shutdown();
    ioExecutor.shutdown();
    computationExecutor.shutdown();
    for (ExecutorService customSchedulerExecutor : customSchedulersExecutors) {
      customSchedulerExecutor.shutdown();
    }
    scheduledExecutor.shutdown();
    try {
      quartzScheduler.shutdown(true);
    } catch (SchedulerException e) {
      throw new LifecycleException(e, this);
    }

    try {
      final long startMillis = currentTimeMillis();

      // Stop the scheduled first to avoid it dispatching tasks to an already stopped executor
      waitForExecutorTermination(startMillis, scheduledExecutor, TIMER_THREADS_NAME);
      waitForExecutorTermination(startMillis, cpuLightExecutor, CPU_LIGHT_THREADS_NAME);
      waitForExecutorTermination(startMillis, ioExecutor, IO_THREADS_NAME);
      waitForExecutorTermination(startMillis, computationExecutor, COMPUTATION_THREADS_NAME);
      for (ExecutorService customSchedulerExecutor : customSchedulersExecutors) {
        waitForExecutorTermination(startMillis, customSchedulerExecutor, COMPUTATION_THREADS_NAME);
      }

      logger.info("Stopped " + this.toString());
    } catch (InterruptedException e) {
      currentThread().interrupt();
      logger.info("Stop of " + this.toString() + " interrupted", e);
    }

    customSchedulersExecutors.clear();
    cpuLightExecutor = null;
    ioExecutor = null;
    computationExecutor = null;
    scheduledExecutor = null;
    quartzScheduler = null;
  }

  protected void waitForExecutorTermination(final long startMillis, final ExecutorService executor, final String executorLabel)
      throws InterruptedException {
    if (!executor.awaitTermination(threadPoolsConfig.getGracefulShutdownTimeout() - (currentTimeMillis() - startMillis),
                                   MILLISECONDS)) {
      final List<Runnable> cancelledJobs = executor.shutdownNow();
      logger.warn("'" + executorLabel + "' " + executor.toString() + " of " + this.toString()
          + " did not shutdown gracefully after " + threadPoolsConfig.getGracefulShutdownTimeout() + " milliseconds.");

      if (logger.isDebugEnabled()) {
        logger.debug("The jobs " + cancelledJobs + " were cancelled.");
      } else {
        logger.info(cancelledJobs.size() + " jobs were cancelled.");
      }
    }
  }

}
