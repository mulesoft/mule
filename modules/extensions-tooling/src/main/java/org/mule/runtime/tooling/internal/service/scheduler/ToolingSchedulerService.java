/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.internal.service.scheduler;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerPoolsConfigFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

/**
 * {@link SchedulerService} implementation that provides a shared {@link ToolingScheduler}.
 *
 * @since 4.0
 */
public class ToolingSchedulerService implements SchedulerService, Stoppable {

  private static final ThreadGroup UNIT_TEST_THREAD_GROUP = new ThreadGroup(ToolingScheduler.class.getSimpleName());
  private static final String CPU_INTENSIVE_SCHEDULER = "cpuIntensiveScheduler";
  private static final String IO_SCHEDULER = "ioScheduler";
  private static final String CPU_LIGHT_SCHEDULER = "cpuLightScheduler";

  private final ToolingScheduler scheduler =
      new ToolingScheduler(8,
                           new NamedThreadFactory(ToolingScheduler.class.getSimpleName(),
                                                  ToolingSchedulerService.class.getClassLoader(),
                                                  UNIT_TEST_THREAD_GROUP),
                           new AbortPolicy());

  private final List<Scheduler> customSchedulers = synchronizedList(new ArrayList<>());
  private final List<Scheduler> decorators = synchronizedList(new ArrayList<>());

  @Override
  public String getName() {
    return SchedulerService.class.getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, CPU_LIGHT_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler ioScheduler() {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, IO_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuIntensiveScheduler() {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, CPU_INTENSIVE_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config) {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, CPU_LIGHT_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler ioScheduler(SchedulerConfig config) {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, IO_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config) {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, CPU_INTENSIVE_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, CPU_LIGHT_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler ioScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, IO_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    final ToolingSchedulerDecorator decorator = decorateScheduler(scheduler, CPU_INTENSIVE_SCHEDULER);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler customScheduler(SchedulerConfig config) {
    final ToolingScheduler customScheduler =
        new ToolingCustomScheduler(config.getMaxConcurrentTasks(),
                                   buildThreadFactory(config),
                                   new AbortPolicy());
    customSchedulers.add(customScheduler);
    final ToolingSchedulerDecorator decorator = decorateScheduler(customScheduler, config.getSchedulerName());
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler customScheduler(SchedulerConfig config, int queueSize) {
    final ToolingScheduler customScheduler =
        new ToolingCustomScheduler(config.getMaxConcurrentTasks(),
                                   buildThreadFactory(config),
                                   new AbortPolicy());
    customSchedulers.add(customScheduler);
    final ToolingSchedulerDecorator decorator = decorateScheduler(customScheduler, config.getSchedulerName());
    decorators.add(decorator);
    return decorator;
  }

  private NamedThreadFactory buildThreadFactory(SchedulerConfig config) {
    return new NamedThreadFactory(config.getSchedulerName() != null
        ? config.getSchedulerName()
        : "SimpleUnitTestSupportSchedulerService_custom") {

      @Override
      public Thread newThread(Runnable runnable) {
        Thread t = new Thread(new ThreadGroup(getName()), runnable);
        configureThread(t);
        return t;
      }
    };
  }

  protected ToolingSchedulerDecorator decorateScheduler(ToolingScheduler scheduler, String name) {
    return withContextClassLoader(ToolingSchedulerService.class.getClassLoader(),
                                  () -> new ToolingSchedulerDecorator(name, scheduler, this));
  }

  @Override
  public void stop() throws MuleException {
    if (!scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    synchronized (customSchedulers) {
      for (Scheduler customScheduler : customSchedulers) {
        customScheduler.shutdownNow();
      }
    }
  }

  @Override
  public List<SchedulerView> getSchedulers() {
    List<SchedulerView> schedulers = new ArrayList<>();

    synchronized (decorators) {
      for (Scheduler scheduler : decorators) {
        schedulers.add(new TestSchedulerView(scheduler));
      }
    }

    return unmodifiableList(schedulers);
  }

  void stoppedScheduler(Scheduler scheduler) {
    decorators.remove(scheduler);

    if (scheduler instanceof ToolingSchedulerDecorator
        && ((ToolingSchedulerDecorator) scheduler)
            .getDecorated() instanceof ToolingCustomScheduler) {
      customSchedulers.remove(((ToolingSchedulerDecorator) scheduler).getDecorated());
    }
  }

  private class TestSchedulerView implements SchedulerView {

    private final Scheduler scheduler;

    /**
     * Creates a reporting view for a {@link Scheduler}.
     *
     * @param scheduler the scheduler to provide a view for.
     */
    public TestSchedulerView(Scheduler scheduler) {
      this.scheduler = scheduler;
    }

    @Override
    public String getName() {
      return scheduler.getName();
    }

    @Override
    public boolean isShutdown() {
      return scheduler.isShutdown();
    }

    @Override
    public boolean isTerminated() {
      return scheduler.isTerminated();
    }

    @Override
    public String toString() {
      return scheduler.toString();
    }
  }
}
