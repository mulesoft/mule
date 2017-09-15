/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static java.util.Collections.unmodifiableList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerPoolsConfigFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

/**
 * {@link SchedulerService} implementation that provides a shared {@link SimpleUnitTestSupportScheduler}.
 *
 * @since 4.0
 */
public class SimpleUnitTestSupportSchedulerService implements SchedulerService, Stoppable {

  private SimpleUnitTestSupportScheduler scheduler =
      new SimpleUnitTestSupportScheduler(8, new NamedThreadFactory(SimpleUnitTestSupportScheduler.class.getSimpleName()),
                                         new AbortPolicy());

  private List<Scheduler> customSchedulers = new ArrayList<>();
  private List<Scheduler> decorators = new ArrayList<>();

  @Override
  public String getName() {
    return SchedulerService.class.getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler ioScheduler() {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuIntensiveScheduler() {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config) {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler ioScheduler(SchedulerConfig config) {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config) {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuLightScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler ioScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler cpuIntensiveScheduler(SchedulerConfig config, SchedulerPoolsConfigFactory poolsConfigFactory) {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(scheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler customScheduler(SchedulerConfig config) {
    final SimpleUnitTestSupportScheduler customScheduler =
        new SimpleUnitTestSupportCustomScheduler(config.getMaxConcurrentTasks(),
                                                 new NamedThreadFactory(config
                                                     .getSchedulerName() != null
                                                         ? config.getSchedulerName()
                                                         : "SimpleUnitTestSupportSchedulerService_custom"),
                                                 new AbortPolicy());
    customSchedulers.add(customScheduler);
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(customScheduler);
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler customScheduler(SchedulerConfig config, int queueSize) {
    final SimpleUnitTestSupportScheduler customScheduler =
        new SimpleUnitTestSupportCustomScheduler(config.getMaxConcurrentTasks(),
                                                 new NamedThreadFactory(config
                                                     .getSchedulerName() != null
                                                         ? config.getSchedulerName()
                                                         : "SimpleUnitTestSupportSchedulerService_custom"),
                                                 new AbortPolicy());
    customSchedulers.add(customScheduler);
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler(customScheduler);
    decorators.add(decorator);
    return decorator;
  }

  protected SimpleUnitTestSupportLifecycleSchedulerDecorator decorateScheduler(SimpleUnitTestSupportScheduler scheduler) {
    SimpleUnitTestSupportLifecycleSchedulerDecorator spied =
        spy(new SimpleUnitTestSupportLifecycleSchedulerDecorator(resolveSchedulerCreationLocation(), scheduler, this));

    doReturn(mock(ScheduledFuture.class)).when(spied).scheduleWithCronExpression(any(), anyString());
    doReturn(mock(ScheduledFuture.class)).when(spied).scheduleWithCronExpression(any(), anyString(), any());
    return spied;
  }

  private String resolveSchedulerCreationLocation() {
    int i = 0;
    final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    StackTraceElement ste = stackTrace[i++];
    // We have to go deep enough, right before the mockito call
    while (skip(ste) && i < stackTrace.length) {
      ste = stackTrace[i++];
    }

    if (skip(ste)) {
      ste = stackTrace[3];
    } else {
      ste = stackTrace[i];
    }

    return ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber();
  }

  private boolean skip(StackTraceElement ste) {
    return ste.getClassName().startsWith(SimpleUnitTestSupportSchedulerService.class.getName())
        || ste.getClassName().startsWith("org.mockito")
        || !ste.getClassName().contains("$Proxy");
  }

  @Override
  public void stop() throws MuleException {
    if (!scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    for (Scheduler customScheduler : customSchedulers) {
      customScheduler.shutdownNow();
    }
  }

  @Override
  public List<SchedulerView> getSchedulers() {
    List<SchedulerView> schedulers = new ArrayList<>();

    for (Scheduler scheduler : decorators) {
      schedulers.add(new TestSchedulerView(scheduler));
    }

    return unmodifiableList(schedulers);
  }

  public void clearCreatedSchedulers() {
    decorators.clear();
  }

  void stoppedScheduler(Scheduler scheduler) {
    decorators.remove(scheduler);
  }

  public int getScheduledTasks() {
    return scheduler.getScheduledTasks();
  }

  private class TestSchedulerView implements SchedulerView {

    private Scheduler scheduler;

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
