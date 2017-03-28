/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.RejectionAction.WAIT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerBusyException;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

@Features("SchedulerService")
public class DefaultSchedulerServiceTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = none();

  private DefaultSchedulerService service;

  @Before
  public void before() throws MuleException {
    service = new DefaultSchedulerService();
    service.start();
  }

  @After
  public void after() throws MuleException {
    if (service == null) {
      return;
    }
    for (Scheduler scheduler : new ArrayList<>(service.getSchedulers())) {
      scheduler.stop(0, SECONDS);
    }
    service.stop();
  }

  @Test
  @Description("Tests that the threads of the SchedulerService are correcly created and destroyed.")
  public void serviceStop() throws MuleException {
    assertThat(collectThreadNames(), hasItem(startsWith(SchedulerService.class.getSimpleName())));

    service.stop();
    service = null;

    new PollingProber(500, 50).check(new JUnitLambdaProbe(() -> {
      assertThat(collectThreadNames(), not(hasItem(startsWith(SchedulerService.class.getSimpleName()))));
      return true;
    }));
  }

  @Test
  @Description("Tests that dispatching a task to a throttled scheduler already running its maximum tasks throws the appropriate exception.")
  public void executorRejects() throws MuleException, ExecutionException, InterruptedException {
    final Latch latch = new Latch();

    final Scheduler cpuLight = service.customScheduler(config().withMaxConcurrentTasks(1));
    final Scheduler custom = service.customScheduler(config().withMaxConcurrentTasks(1));

    custom.execute(() -> {
      try {
        latch.await();
      } catch (InterruptedException e) {
        currentThread().interrupt();
      }
    });

    expected.expect(ExecutionException.class);
    expected.expectCause(instanceOf(SchedulerBusyException.class));

    final Runnable task = () -> {
    };
    cpuLight.submit(() -> {
      try {
        custom.submit(task);
      } finally {
        assertThat(custom.shutdownNow(), not(hasItem(task)));
      }
    }).get();
  }

  @Test
  @Description("Tests that a dispatched task has inherited the context classloader.")
  public void classLoaderPropagates() throws Exception {
    final Scheduler scheduler = service.cpuLightScheduler();

    final ClassLoader contextClassLoader = mock(ClassLoader.class);
    currentThread().setContextClassLoader(contextClassLoader);

    final Future<?> submit = scheduler.submit(() -> {
      assertThat(currentThread().getContextClassLoader(), sameInstance(contextClassLoader));
    });

    submit.get(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);
  }

  @Test
  @Description("Tests that a scheduled task has inherited the context classloader.")
  public void classLoaderPropagatesScheduled() throws Exception {
    final Scheduler scheduler = service.cpuLightScheduler();

    final ClassLoader contextClassLoader = mock(ClassLoader.class);
    currentThread().setContextClassLoader(contextClassLoader);

    Latch latch = new Latch();
    ScheduledFuture<?> submit = null;
    try {
      submit = scheduler.scheduleWithFixedDelay(() -> {
        assertThat(currentThread().getContextClassLoader(), sameInstance(contextClassLoader));
        latch.countDown();
      }, 0, 60, SECONDS);

      latch.await(10, SECONDS);
      submit.get(10, SECONDS);
    } finally {
      if (submit != null) {
        submit.cancel(false);
      }
    }
  }

  @Test
  @Description("Tests that a cron-scheduled task has inherited the context classloader.")
  public void classLoaderPropagatesCron() throws Exception {
    final Scheduler scheduler = service.cpuLightScheduler();

    final ClassLoader contextClassLoader = mock(ClassLoader.class);
    currentThread().setContextClassLoader(contextClassLoader);

    Latch latch = new Latch();
    ScheduledFuture<?> submit = null;
    try {
      submit = scheduler.scheduleWithCronExpression(() -> {
        assertThat(currentThread().getContextClassLoader(), sameInstance(contextClassLoader));
        latch.countDown();
      }, "*/1 * * ? * *");

      latch.await(10, SECONDS);
      submit.get(10, SECONDS);
    } finally {
      if (submit != null) {
        submit.cancel(false);
      }
    }
  }

  @Test
  public void onlyCustomMayConfigureWaitCpuLight() {
    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Only custom schedulers may define waitDispatchingToBusyScheduler");
    service.cpuLightScheduler(config().withRejectionAction(WAIT));
  }

  @Test
  public void onlyCustomMayConfigureWaitCpuIntensive() {
    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Only custom schedulers may define waitDispatchingToBusyScheduler");
    service.cpuIntensiveScheduler(config().withRejectionAction(WAIT));
  }

  @Test
  public void onlyCustomMayConfigureWaitIO() {
    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Only custom schedulers may define waitDispatchingToBusyScheduler");
    service.ioScheduler(config().withRejectionAction(WAIT));
  }

  @Test
  @Description("Tests that tasks dispatched from a CPU Light thread to a busy Scheduler are rejected.")
  public void rejectionPolicyCpuLight() throws MuleException, InterruptedException, ExecutionException, TimeoutException {
    Scheduler sourceScheduler = service.cpuLightScheduler();
    Scheduler targetScheduler = service.customScheduler(config().withMaxConcurrentTasks(1));

    Latch latch = new Latch();

    Future<Object> submit = sourceScheduler.submit(threadsConsumer(targetScheduler, latch));

    expected.expect(ExecutionException.class);
    expected.expectCause(instanceOf(SchedulerBusyException.class));
    submit.get(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);
  }

  @Test
  @Description("Tests that tasks dispatched from a CPU Intensive thread to a busy Scheduler are rejected.")
  public void rejectionPolicyCpuIntensive() throws MuleException, InterruptedException, ExecutionException, TimeoutException {
    Scheduler sourceScheduler = service.cpuIntensiveScheduler();
    Scheduler targetScheduler = service.customScheduler(config().withMaxConcurrentTasks(1));

    Latch latch = new Latch();

    Future<Object> submit = sourceScheduler.submit(threadsConsumer(targetScheduler, latch));

    expected.expect(ExecutionException.class);
    expected.expectCause(instanceOf(SchedulerBusyException.class));
    submit.get(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);
  }

  @Test
  @Description("Tests that tasks dispatched from an IO thread to a busy Scheduler waits for execution.")
  public void rejectionPolicyIO() throws MuleException, InterruptedException, ExecutionException, TimeoutException {
    Scheduler sourceScheduler = service.ioScheduler();
    Scheduler targetScheduler = service.customScheduler(config().withMaxConcurrentTasks(1));

    Latch latch = new Latch();

    Future<Object> submit = sourceScheduler.submit(threadsConsumer(targetScheduler, latch));

    try {
      submit.get(1, SECONDS);
      fail();
    } catch (TimeoutException te) {
    }

    latch.countDown();
    submit.get(5, SECONDS);
  }

  @Test
  @Description("Tests that periodic tasks scheduled to a busy Scheduler are skipped but the job continues executing.")
  public void rejectionPolicyScheduledPeriodic()
      throws MuleException, InterruptedException, ExecutionException, TimeoutException {
    Scheduler sourceScheduler = service.customScheduler(config().withMaxConcurrentTasks(2));
    Scheduler targetScheduler = service.cpuLightScheduler();

    Latch latch = new Latch();

    Future<Object> submit = sourceScheduler.submit(threadsConsumer(targetScheduler, latch));

    try {
      submit.get(1, SECONDS);
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause(), instanceOf(SchedulerBusyException.class));
    }

    CountDownLatch scheduledTaskLatch = new CountDownLatch(2);
    AtomicReference<ScheduledFuture> scheduledTask = new AtomicReference<ScheduledFuture>(null);

    sourceScheduler.submit(() -> {
      scheduledTask.set(targetScheduler.scheduleWithFixedDelay(() -> {
        scheduledTaskLatch.countDown();
      }, 0, 1, SECONDS));
      return null;
    });

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      assertThat(scheduledTask.get().isDone(), is(true));
      return true;
    }));
    latch.countDown();

    assertThat(scheduledTaskLatch.await(5, SECONDS), is(true));
  }

  @Test
  @Description("Tests that tasks dispatched from a Custom scheduler thread to a busy Scheduler waits for execution.")
  public void rejectionPolicyCustom() throws MuleException, InterruptedException, ExecutionException, TimeoutException {
    Scheduler sourceScheduler = service.customScheduler(config().withMaxConcurrentTasks(1));
    Scheduler targetScheduler = service.customScheduler(config().withMaxConcurrentTasks(1));

    Latch latch = new Latch();

    Future<Object> submit = sourceScheduler.submit(threadsConsumer(targetScheduler, latch));

    expected.expect(ExecutionException.class);
    expected.expectCause(instanceOf(SchedulerBusyException.class));
    submit.get(DEFAULT_TEST_TIMEOUT_SECS, SECONDS);
  }

  @Test
  @Description("Tests that tasks dispatched from a Custom scheduler with 'Wait' allowed thread to a busy Scheduler waits for execution.")
  public void rejectionPolicyCustomWithConfig() throws MuleException, InterruptedException, ExecutionException, TimeoutException {
    Scheduler sourceScheduler =
        service.customScheduler(config().withRejectionAction(WAIT).withMaxConcurrentTasks(1), 1);
    Scheduler targetScheduler = service.customScheduler(config().withMaxConcurrentTasks(1));

    Latch latch = new Latch();

    Future<Object> submit = sourceScheduler.submit(threadsConsumer(targetScheduler, latch));

    try {
      submit.get(1, SECONDS);
      fail();
    } catch (TimeoutException te) {
    }

    latch.countDown();
    submit.get(5, SECONDS);
  }

  @Test
  @Description("Tests that tasks dispatched from any other thread to a busy Scheduler are rejected.")
  public void rejectionPolicyOther() throws MuleException, InterruptedException, ExecutionException, TimeoutException {
    ExecutorService sourceExecutor = newSingleThreadExecutor();
    Scheduler targetScheduler = service.customScheduler(config().withMaxConcurrentTasks(1));

    Latch latch = new Latch();

    Future<Object> submit = sourceExecutor.submit(threadsConsumer(targetScheduler, latch));

    try {
      submit.get(1, SECONDS);
      fail();
    } catch (TimeoutException te) {
    }

    latch.countDown();
    submit.get(5, SECONDS);
  }

  private Callable<Object> threadsConsumer(Scheduler targetScheduler, Latch latch) {
    return () -> {
      while (latch.getCount() > 0) {
        targetScheduler.submit(() -> {
          try {
            latch.await();
          } catch (InterruptedException e) {
            currentThread().interrupt();
          }
        });
      }
      return null;
    };
  }
}
