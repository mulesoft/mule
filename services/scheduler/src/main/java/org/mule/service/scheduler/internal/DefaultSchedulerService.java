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
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.RejectionAction.DEFAULT;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.RejectionAction.WAIT;
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
import org.mule.runtime.core.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.service.scheduler.ThreadType;
import org.mule.service.scheduler.internal.config.ThreadPoolsConfig;
import org.mule.service.scheduler.internal.executor.ByCallerThreadGroupPolicy;
import org.mule.service.scheduler.internal.threads.SchedulerThreadFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;

/**
 * Default implementation of {@link SchedulerService}.
 * <p>
 * {@link Scheduler}s provided by this implementation of {@link SchedulerService} use a shared single-threaded
 * {@link ScheduledExecutorService} for scheduling work. When a scheduled tasks is fired, they are executed using the
 * {@link Scheduler}'s own executor.
 *
 * @since 4.0
 */
public class DefaultSchedulerService implements SchedulerService, Startable, Stoppable {

  private static final Logger logger = getLogger(DefaultSchedulerService.class);

  private static final long DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 5000;

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
  private final ThreadGroup customWaitGroup = new ThreadGroup(customGroup, CUSTOM_THREADS_NAME);

  private final RejectedExecutionHandler byCallerThreadGroupPolicy =
      new ByCallerThreadGroupPolicy(new HashSet<>(asList(ioGroup, customWaitGroup)), schedulerGroup);

  private ThreadPoolExecutor cpuLightExecutor;
  private ThreadPoolExecutor ioExecutor;
  private ThreadPoolExecutor computationExecutor;
  private Set<ExecutorService> customSchedulersExecutors = new HashSet<>();
  private ScheduledThreadPoolExecutor scheduledExecutor;
  private org.quartz.Scheduler quartzScheduler;

  private volatile boolean started = false;
  private List<Scheduler> activeSchedulers = synchronizedList(new ArrayList<>());

  @Override
  public String getName() {
    return SchedulerService.class.getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return doCpuLightScheduler(config());
  }

  @Override
  public Scheduler ioScheduler() {
    return doIoScheduler(config());
  }

  @Override
  public Scheduler cpuIntensiveScheduler() {
    return doCpuIntensiveScheduler(config());
  }

  @Override
  @Inject
  public Scheduler cpuLightScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    return doCpuLightScheduler(config);
  }

  @Override
  @Inject
  public Scheduler ioScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    return doIoScheduler(config);
  }

  @Override
  @Inject
  public Scheduler cpuIntensiveScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    return doCpuIntensiveScheduler(config);
  }

  private Scheduler doCpuLightScheduler(SchedulerConfig config) {
    checkStarted();
    if (config.getRejectionAction() != DEFAULT) {
      throw new IllegalArgumentException("Only custom schedulers may define waitDispatchingToBusyScheduler");
    }
    final String schedulerName = resolveSchedulerName(config, CPU_LIGHT_THREADS_NAME);
    Scheduler scheduler;
    if (config.getMaxConcurrentTasks() != null) {
      scheduler =
          new ThrottledScheduler(schedulerName, cpuLightExecutor, 4 * cores, scheduledExecutor, quartzScheduler, CPU_LIGHT,
                                 config.getMaxConcurrentTasks(), resolveStopTimeout(config),
                                 schr -> activeSchedulers.remove(schr));
    } else {
      scheduler = new DefaultScheduler(schedulerName, cpuLightExecutor, 4 * cores, scheduledExecutor, quartzScheduler, CPU_LIGHT,
                                       resolveStopTimeout(config), schr -> activeSchedulers.remove(schr));
    }
    activeSchedulers.add(scheduler);
    return scheduler;
  }

  private Scheduler doIoScheduler(SchedulerConfig config) {
    checkStarted();
    if (config.getRejectionAction() != DEFAULT) {
      throw new IllegalArgumentException("Only custom schedulers may define waitDispatchingToBusyScheduler");
    }
    final String schedulerName = resolveSchedulerName(config, IO_THREADS_NAME);
    Scheduler scheduler;
    if (config.getMaxConcurrentTasks() != null) {
      scheduler = new ThrottledScheduler(schedulerName, ioExecutor, cores * cores, scheduledExecutor, quartzScheduler, IO,
                                         config.getMaxConcurrentTasks(), resolveStopTimeout(config),
                                         schr -> activeSchedulers.remove(schr));
    } else {
      scheduler = new DefaultScheduler(schedulerName, ioExecutor, cores * cores, scheduledExecutor, quartzScheduler, IO,
                                       resolveStopTimeout(config), schr -> activeSchedulers.remove(schr));
    }
    activeSchedulers.add(scheduler);
    return scheduler;
  }

  private Scheduler doCpuIntensiveScheduler(SchedulerConfig config) {
    checkStarted();
    if (config.getRejectionAction() != DEFAULT) {
      throw new IllegalArgumentException("Only custom schedulers may define waitDispatchingToBusyScheduler");
    }
    final String schedulerName = resolveSchedulerName(config, COMPUTATION_THREADS_NAME);
    Scheduler scheduler;
    if (config.getMaxConcurrentTasks() != null) {
      scheduler = new ThrottledScheduler(schedulerName, computationExecutor, 4 * cores, scheduledExecutor, quartzScheduler,
                                         CPU_INTENSIVE, config.getMaxConcurrentTasks(),
                                         resolveStopTimeout(config),
                                         schr -> activeSchedulers.remove(schr));
    } else {
      scheduler =
          new DefaultScheduler(schedulerName, computationExecutor, 4 * cores, scheduledExecutor, quartzScheduler, CPU_INTENSIVE,
                               resolveStopTimeout(config), schr -> activeSchedulers.remove(schr));
    }
    activeSchedulers.add(scheduler);
    return scheduler;
  }

  @Override
  @Inject
  public Scheduler customScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config) {
    return doCustomScheduler(config);
  }

  @Override
  @Inject
  public Scheduler customScheduler(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig config, int queueSize) {
    return doCustomScheduler(config, queueSize);
  }

  private Scheduler doCustomScheduler(SchedulerConfig config) {
    checkStarted();
    if (config.getMaxConcurrentTasks() == null) {
      throw new IllegalArgumentException("Custom schedulers must define a thread pool size");
    }
    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(config.getMaxConcurrentTasks(), config.getMaxConcurrentTasks(), 0L, MILLISECONDS,
                               new SynchronousQueue<Runnable>(),
                               new SchedulerThreadFactory(resolveThreadGroupForCustomScheduler(config),
                                                          "%s." + resolveSchedulerName(config, CUSTOM_THREADS_NAME) + ".%02d"),
                               byCallerThreadGroupPolicy);

    final DefaultScheduler customScheduler =
        new CustomScheduler(resolveSchedulerName(config, CUSTOM_THREADS_NAME), executor, cores,
                            scheduledExecutor, quartzScheduler, CUSTOM, resolveStopTimeout(config),
                            schr -> activeSchedulers.remove(schr));
    activeSchedulers.add(customScheduler);
    return customScheduler;
  }

  private Scheduler doCustomScheduler(SchedulerConfig config, int queueSize) {
    checkStarted();
    if (config.getMaxConcurrentTasks() == null) {
      throw new IllegalArgumentException("Custom schedulers must define a thread pool size");
    }
    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(config.getMaxConcurrentTasks(), config.getMaxConcurrentTasks(), 0L, MILLISECONDS,
                               new LinkedBlockingQueue<Runnable>(queueSize),
                               new SchedulerThreadFactory(resolveThreadGroupForCustomScheduler(config),
                                                          "%s." + resolveSchedulerName(config, CUSTOM_THREADS_NAME) + ".%02d"),
                               byCallerThreadGroupPolicy);

    final DefaultScheduler customScheduler =
        new CustomScheduler(resolveSchedulerName(config, CUSTOM_THREADS_NAME), executor, cores, scheduledExecutor,
                            quartzScheduler, CUSTOM, resolveStopTimeout(config),
                            schr -> activeSchedulers.remove(schr));
    customSchedulersExecutors.add(customScheduler);
    activeSchedulers.add(customScheduler);
    return customScheduler;
  }

  private long resolveStopTimeout(SchedulerConfig config) {
    return config.getShutdownTimeoutMillis() != null ? config.getShutdownTimeoutMillis() : DEFAULT_SHUTDOWN_TIMEOUT_MILLIS;
  }

  private String resolveSchedulerName(SchedulerConfig config, String prefix) {
    if (config.getSchedulerName() == null) {
      return resolveSchedulerCreationLocation(prefix);
    } else {
      return config.getSchedulerName();
    }
  }

  private ThreadGroup resolveThreadGroupForCustomScheduler(SchedulerConfig config) {
    if (config.getRejectionAction() == WAIT) {
      return customWaitGroup;
    } else {
      return customGroup;
    }
  }

  private void checkStarted() {
    if (!started) {
      throw new IllegalStateException("Service " + getName() + " is not started.");
    }
  }

  private class CustomScheduler extends DefaultScheduler {

    private final ExecutorService executor;

    private CustomScheduler(String name, ExecutorService executor, int workers, ScheduledExecutorService scheduledExecutor,
                            org.quartz.Scheduler quartzScheduler, ThreadType threadsType, long shutdownTimeoutMillis,
                            Consumer<Scheduler> shutdownCallback) {
      super(name, executor, workers, scheduledExecutor, quartzScheduler, threadsType, shutdownTimeoutMillis, shutdownCallback);
      this.executor = executor;
    }

    @Override
    public void shutdown() {
      super.shutdown();
      executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
      customSchedulersExecutors.remove(this);

      final List<Runnable> cancelledTasks = super.shutdownNow();
      executor.shutdownNow();
      return cancelledTasks;
    }
  }

  private String resolveSchedulerCreationLocation(String prefix) {
    int i = 0;
    final StackTraceElement[] stackTrace = new Throwable().getStackTrace();

    StackTraceElement ste = stackTrace[i++];
    // We have to go deep enough, right before the proxy calls
    while (skip(ste) && i < stackTrace.length) {
      ste = stackTrace[i++];
    }

    if (skip(ste)) {
      ste = stackTrace[3];
    } else {
      ste = stackTrace[i];
    }

    return prefix + "@" + (ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber());
  }

  private boolean skip(StackTraceElement ste) {
    return !ste.getClassName().contains("$Proxy");
  }

  @Override
  public void start() throws MuleException {
    logger.info("Starting " + this.toString() + "...");

    threadPoolsConfig = loadThreadPoolsConfig();

    cpuLightExecutor = new ThreadPoolExecutor(threadPoolsConfig.getCpuLightPoolSize(), threadPoolsConfig.getCpuLightPoolSize(),
                                              0, SECONDS, new LinkedBlockingQueue<>(threadPoolsConfig.getCpuLightQueueSize()),
                                              new SchedulerThreadFactory(cpuLightGroup), byCallerThreadGroupPolicy);
    ioExecutor = new ThreadPoolExecutor(threadPoolsConfig.getIoCorePoolSize(), threadPoolsConfig.getIoMaxPoolSize(),
                                        threadPoolsConfig.getIoKeepAlive(), MILLISECONDS,
                                        // TODO MULE-11505 - Implement cached IO scheduler that grows and uses async hand-off
                                        // with queue.
                                        new SynchronousQueue<>(),
                                        new SchedulerThreadFactory(ioGroup), byCallerThreadGroupPolicy);
    computationExecutor =
        new ThreadPoolExecutor(threadPoolsConfig.getCpuIntensivePoolSize(), threadPoolsConfig.getCpuIntensivePoolSize(),
                               0, SECONDS, new LinkedBlockingQueue<>(threadPoolsConfig.getCpuIntensiveQueueSize()),
                               new SchedulerThreadFactory(computationGroup), byCallerThreadGroupPolicy);

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

    cpuLightExecutor.prestartAllCoreThreads();
    ioExecutor.prestartAllCoreThreads();
    computationExecutor.prestartAllCoreThreads();
    scheduledExecutor.prestartAllCoreThreads();

    logger.info("Started " + this.toString());
    started = true;
  }

  @Override
  public void stop() throws MuleException {
    started = false;
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

      // When graceful shutdown timeouts, forceful shutdown will remove the custom scheduler from the list.
      // In that case, not creating a new collection here will cause a ConcurrentModificationException.
      for (ExecutorService customSchedulerExecutor : new ArrayList<>(customSchedulersExecutors)) {
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

  @Override
  public List<Scheduler> getSchedulers() {
    // TODO MULE-10549 Improve this syncronization
    synchronized (activeSchedulers) {
      return unmodifiableList(new ArrayList<>(activeSchedulers));
    }
  }
}
