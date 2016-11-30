/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal;

import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import static org.mule.service.scheduler.ThreadType.CUSTOM;

import org.mule.runtime.core.util.concurrent.NamedThreadFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class BaseDefaultSchedulerTestCase extends AbstractMuleTestCase {

  protected static final int DELTA_MILLIS = 30;
  protected static final int EXECUTOR_TIMEOUT_SECS = 1;

  protected static final Runnable EMPTY_RUNNABLE = () -> {
  };
  protected static final Consumer<ScheduledExecutorService> SUBMIT_EMPTY_CALLABLE = exec -> exec.submit(() -> 0);
  protected static final Consumer<ScheduledExecutorService> SUBMIT_EMPTY_RUNNABLE = exec -> exec.submit(EMPTY_RUNNABLE);
  protected static final Consumer<ScheduledExecutorService> SUBMIT_RESULT_RUNNABLE = exec -> exec.submit(EMPTY_RUNNABLE, 0);
  protected static final Consumer<ScheduledExecutorService> EXECUTE_EMPTY_RUNNABLE = exec -> exec.execute(EMPTY_RUNNABLE);


  @Rule
  public ExpectedException expected = ExpectedException.none();

  protected ExecutorService sharedExecutor;
  protected ScheduledThreadPoolExecutor sharedScheduledExecutor;
  protected org.quartz.Scheduler sharedQuartzScheduler;

  @Before
  public void before() throws SchedulerException {
    sharedExecutor =
        new ThreadPoolExecutor(1, 1, 0, SECONDS, new ArrayBlockingQueue<>(1), new NamedThreadFactory(this.getClass().getName()));

    sharedScheduledExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(this.getClass().getName() + "_sched"));
    sharedScheduledExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    sharedScheduledExecutor.setRemoveOnCancelPolicy(true);

    StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
    schedulerFactory.initialize(defaultQuartzProperties());
    sharedQuartzScheduler = schedulerFactory.getScheduler();
    sharedQuartzScheduler.start();
  }

  private Properties defaultQuartzProperties() {
    Properties factoryProperties = new Properties();

    factoryProperties.setProperty("org.quartz.scheduler.instanceName", getClass().getSimpleName());
    factoryProperties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    factoryProperties.setProperty("org.quartz.threadPool.threadNamePrefix", getClass().getSimpleName() + "_qz");
    factoryProperties.setProperty("org.quartz.threadPool.threadCount", "1");
    return factoryProperties;
  }

  @After
  public void after() throws SchedulerException {
    sharedExecutor.shutdownNow();
    sharedScheduledExecutor.shutdownNow();
    sharedQuartzScheduler.shutdown();
  }

  protected void assertTerminationIsNotDelayed(final ScheduledExecutorService executor) throws InterruptedException {
    long startTime = nanoTime();
    executor.shutdown();
    executor.awaitTermination(1000, MILLISECONDS);

    assertThat((double) NANOSECONDS.toMillis(nanoTime() - startTime), closeTo(0, DELTA_MILLIS));
  }

  protected ScheduledExecutorService createExecutor() {
    return new DefaultScheduler(BaseDefaultSchedulerTestCase.class.getSimpleName(), sharedExecutor, 1, sharedScheduledExecutor,
                                sharedQuartzScheduler, CUSTOM);
  }

  protected boolean awaitLatch(final CountDownLatch latch) {
    try {
      return latch.await(10 * EXECUTOR_TIMEOUT_SECS, SECONDS);
    } catch (InterruptedException e) {
      currentThread().interrupt();
      return false;
    }
  }

}
