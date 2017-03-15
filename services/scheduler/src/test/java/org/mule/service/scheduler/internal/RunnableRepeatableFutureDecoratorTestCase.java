/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.quartz.SchedulerException;

public class RunnableRepeatableFutureDecoratorTestCase extends BaseDefaultSchedulerTestCase {

  @Rule
  public ExpectedException expected = none();

  private DefaultScheduler scheduler;

  private RunnableRepeatableFutureDecorator<Object> taskDecorator;

  @Override
  @Before
  public void before() throws SchedulerException {
    super.before();
    scheduler = (DefaultScheduler) createExecutor();
  }

  @Override
  @After
  public void after() throws SchedulerException, InterruptedException {
    scheduler.stop(5, SECONDS);
    scheduler = null;
    super.after();
  }

  @Test
  public void exceptionInWrapUpCallbackCompletesWrapUp() {
    final ClassLoader taskClassloader = mock(ClassLoader.class);
    taskDecorator =
        new RunnableRepeatableFutureDecorator<>(() -> new FutureTask<>(() -> {
          return null;
        }), d -> {
          throw new WrapUpException();
        }, taskClassloader, scheduler, "testTask", -1);

    expected.expect(WrapUpException.class);
    try {
      taskDecorator.run();
    } finally {
      assertThat(taskDecorator.isStarted(), is(false));
      assertThat(currentThread().getContextClassLoader(), not(taskClassloader));
    }
  }

  @Test
  public void repeatableSecondRunBeforeFirstWrapUp() {
    final AtomicInteger runCount = new AtomicInteger(0);

    taskDecorator =
        new RunnableRepeatableFutureDecorator<>(() -> new FutureTask<>(() -> {
          runCount.incrementAndGet();
          return null;
        }), d -> {
          if (runCount.get() < 2) {
            taskDecorator.run();
          }
        }, RunnableRepeatableFutureDecoratorTestCase.class.getClassLoader(), scheduler, "testTask", -1);

    taskDecorator.run();

    assertThat(taskDecorator.isStarted(), is(false));
    assertThat(runCount.get(), is(2));
  }

  private static class WrapUpException extends RuntimeException {

    private static final long serialVersionUID = 5170908600838156528L;

  }
}
