/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class AbstractProcessingStrategyTestCase extends AbstractReactiveProcessorTestCase {

  protected static final String CPU_LIGHT = "cpuLight";
  protected static final String IO = "I/O";
  protected static final String CPU_INTENSIVE = "cpuIntensive";

  protected Flow flow;
  protected volatile Set<String> threads = new HashSet<>();
  protected Processor cpuLightProcessor = new ThreadTrackingProcessor() {

    @Override
    public ProcessingType getProcessingType() {
      return CPU_LITE;
    }
  };
  protected Processor cpuIntensiveProcessor = new ThreadTrackingProcessor() {

    @Override
    public ProcessingType getProcessingType() {
      return ProcessingType.CPU_INTENSIVE;
    }
  };
  protected Processor blockingProcessor = new ThreadTrackingProcessor() {

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }
  };

  protected Scheduler cpuLight;
  protected Scheduler blocking;
  protected Scheduler cpuIntensive;
  protected Scheduler custom;
  private ExecutorService asyncExecutor;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  public AbstractProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Before
  public void before() throws RegistrationException {
    cpuLight = new TestScheduler(3, CPU_LIGHT);
    blocking = new TestScheduler(3, IO);
    cpuIntensive = new TestScheduler(3, CPU_INTENSIVE);
    custom = new TestScheduler(10, CPU_LIGHT);
    asyncExecutor = muleContext.getRegistry().lookupObject(SchedulerService.class).ioScheduler();

    flow = builder("test", muleContext)
        .processingStrategyFactory((muleContext, prefix) -> createProcessingStrategy(muleContext, prefix)).build();
  }

  protected abstract ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix);

  @After
  public void after() {
    flow.dispose();
    cpuLight.shutdownNow();
    blocking.shutdownNow();
    cpuIntensive.shutdownNow();
    asyncExecutor.shutdownNow();
  }

  @Test
  public void singleCpuLight() throws Exception {
    flow.setMessageProcessors(singletonList(cpuLightProcessor));
    flow.initialise();
    flow.start();
    process(flow, testEvent());
  }

  @Test
  public void singleCpuLightConcurrent() throws Exception {
    internalSingleCpuLightConcurrent(false);
  }

  protected void internalSingleCpuLightConcurrent(boolean blocks) throws MuleException, InterruptedException {
    FirstInvocationLatchedProcessor latchedProcessor = new FirstInvocationLatchedProcessor(CPU_LITE);

    flow.setMessageProcessors(singletonList(latchedProcessor));
    flow.initialise();
    flow.start();

    asyncExecutor.submit(() -> process(flow, testEvent()));

    latchedProcessor.getFirstCalledLatch().await();

    asyncExecutor.submit(() -> process(flow, testEvent()));
    assertThat(latchedProcessor.getSecondCalledLatch().await(BLOCK_TIMEOUT, MILLISECONDS), is(!blocks));

    latchedProcessor.releaseFirst();

    if (blocks) {
      assertThat(latchedProcessor.getSecondCalledLatch().await(BLOCK_TIMEOUT, MILLISECONDS), is(true));
    }

  }

  @Test
  public void multipleCpuLight() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuLightProcessor, cpuLightProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void singleBlocking() throws Exception {
    flow.setMessageProcessors(singletonList(blockingProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void multipleBlocking() throws Exception {
    flow.setMessageProcessors(asList(blockingProcessor, blockingProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void singleCpuIntensive() throws Exception {
    flow.setMessageProcessors(singletonList(cpuIntensiveProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void multipleCpuIntensive() throws Exception {
    flow.setMessageProcessors(asList(cpuIntensiveProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void mix() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void mix2() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuLightProcessor, blockingProcessor, blockingProcessor,
                                     cpuLightProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor, cpuLightProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public abstract void tx() throws Exception;

  class FirstInvocationLatchedProcessor implements Processor {

    private ProcessingType type;
    private volatile Latch latch = new Latch();
    private volatile Latch firstCalledLatch = new Latch();
    private volatile Latch secondCalledLatch = new Latch();
    private AtomicBoolean firstCalled = new AtomicBoolean();

    public FirstInvocationLatchedProcessor(ProcessingType type) {
      this.type = type;
    }

    @Override
    public Event process(Event event) throws MuleException {
      threads.add(currentThread().getName());
      if (firstCalled.compareAndSet(false, true)) {
        firstCalledLatch.release();
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      } else {
        secondCalledLatch.countDown();
      }

      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return type;
    }

    public void releaseFirst() {
      latch.release();
    }

    public CountDownLatch getFirstCalledLatch() throws InterruptedException {
      return firstCalledLatch;
    }

    public CountDownLatch getSecondCalledLatch() throws InterruptedException {
      return secondCalledLatch;
    }

  }

  static class TestScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

    public TestScheduler(int threads, String threadNamePrefix) {
      super(threads, new NamedThreadFactory(threadNamePrefix));
    }

    @Override
    public Future<?> submit(Runnable task) {
      return super.submit(task);
    }

    @Override
    public void stop(long gracefulShutdownTimeoutSecs, TimeUnit unit) {
      // Nothing to do.
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
      return TestScheduler.class.getSimpleName();
    }

  }

  class ThreadTrackingProcessor implements Processor {

    @Override
    public Event process(Event event) {
      threads.add(currentThread().getName());
      return event;
    }

  }
}
