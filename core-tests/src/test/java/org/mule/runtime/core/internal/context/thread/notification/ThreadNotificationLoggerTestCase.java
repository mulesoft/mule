/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ACTIVATE_SCHEDULERS_LATENCY_REPORT;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadNotificationLoggerTestCase {

  @ClassRule
  public static SystemProperty logging = new SystemProperty(MULE_ACTIVATE_SCHEDULERS_LATENCY_REPORT, "true");

  private static final String IO = "io";
  private static final String CPU_LIGHT = "cpuLight";
  private ThreadNotificationLogger logger;
  private ThreadNotificationService service = mock(ThreadNotificationService.class);
  private CoreEvent event = mock(CoreEvent.class);
  private EventContext context = mock(EventContext.class);
  private Scheduler scheduler = new TestScheduler(2, "test", IO);
  private Scheduler scheduler2 = new TestScheduler(2, "test", CPU_LIGHT);

  @Before
  public void setup() {
    logger = new ThreadNotificationLogger(service);
    when(event.getContext()).thenReturn(context);
    when(context.getId()).thenReturn("id");
  }

  @Test
  public void withoutThreadSwitch() {
    Reference<Boolean> checked = new Reference<>(false);
    just(event)
        .doOnNext(coreEvent -> logger.setStartingThread(event))
        .map(coreEvent -> coreEvent)
        .doOnNext(coreEvent -> logger.setFinishThread(event))
        .doOnNext(coreEvent -> {
          verify(service, never()).addThreadNotificationElement(any());
          checked.set(true);
        }).subscribe();

    new PollingProber().check(new JUnitLambdaProbe(() -> checked.get()));
  }

  @Test
  public void withPublishOnThreadSwitch() {
    reactor.core.scheduler.Scheduler publishOnScheduler = fromExecutorService(scheduler);

    List<ThreadNotificationService.ThreadNotificationElement> notifications = new ArrayList<>();
    doAnswer(invocationOnMock -> {
      notifications.add((ThreadNotificationService.ThreadNotificationElement) invocationOnMock.getArguments()[0]);
      return null;
    }).when(service).addThreadNotificationElement(any());

    just(event)
        .doOnNext(coreEvent -> logger.setStartingThread(event))
        .publishOn(publishOnScheduler)
        .map(coreEvent -> coreEvent)
        .doOnNext(coreEvent -> logger.setFinishThread(event))
        .subscribe();

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      verify(service, times(1)).addThreadNotificationElement(any());
      assertThat(notifications, hasSize(1));
      assertThat(notifications.get(0).getFromThreadType(), not(is(IO)));
      assertThat(notifications.get(0).getToThreadType(), is(IO));
      return true;
    }));
  }

  @Test
  public void withSubscribeOnThreadSwitch() {
    reactor.core.scheduler.Scheduler subscribeOnScheduler =
        fromExecutorService(new ThreadLoggingExecutorServiceDecorator(of(logger), scheduler2, event));

    List<ThreadNotificationService.ThreadNotificationElement> notifications = new ArrayList<>();
    doAnswer(invocationOnMock -> {
      notifications.add((ThreadNotificationService.ThreadNotificationElement) invocationOnMock.getArguments()[0]);
      return null;
    }).when(service).addThreadNotificationElement(any());

    just(event)
        .map(e -> e)
        .subscribeOn(subscribeOnScheduler)
        .map(coreEvent -> coreEvent)
        .doOnNext(e -> logger.setFinishThread(e))
        .subscribe();

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      verify(service, times(1)).addThreadNotificationElement(any());
      assertThat(notifications, hasSize(1));
      assertThat(notifications.get(0).getFromThreadType(), not(is(CPU_LIGHT)));
      assertThat(notifications.get(0).getToThreadType(), is(CPU_LIGHT));
      return true;
    }));
  }

  class TestScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

    private String threadNamePrefix;
    private ExecutorService executor;

    public TestScheduler(int threads, String threadNamePrefix, String threadGroupName) {
      super(1, new NamedThreadFactory(threadNamePrefix + ".tasks"));
      this.threadNamePrefix = threadNamePrefix;
      executor = new ThreadPoolExecutor(threads, threads, 0l, TimeUnit.MILLISECONDS,
                                        new LinkedBlockingQueue(Integer.MAX_VALUE),
                                        new NamedThreadFactory(threadNamePrefix, null, new ThreadGroup(threadGroupName)));
    }

    @Override
    public Future<?> submit(Runnable task) {
      return executor.submit(task);
    }

    @Override
    public Future<?> submit(Callable task) {
      return executor.submit(task);
    }

    @Override
    public void stop() {
      shutdown();
      executor.shutdown();
    }

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
      throw new UnsupportedOperationException(
                                              "Cron expression scheduling is not supported in unit tests. You need the productive service implementation.");
    }

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
      throw new UnsupportedOperationException(
                                              "Cron expression scheduling is not supported in unit tests. You need the productive service implementation.");
    }

    @Override
    public String getName() {
      return threadNamePrefix;
    }

  }

}
