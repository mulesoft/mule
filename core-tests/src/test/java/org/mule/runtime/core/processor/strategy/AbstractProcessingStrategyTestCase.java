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
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.ThreadType;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
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

  protected Flow flow;
  protected volatile Set<String> threads = new HashSet<>();
  protected Processor cpuLightProcessor = event -> {
    threads.add(currentThread().getName());
    return event;
  };
  protected Processor cpuIntensiveProcessor = new Processor() {

    @Override
    public Event process(Event event) {
      threads.add(currentThread().getName());
      return event;
    }

    @Override
    public ProcessingType getProccesingType() {
      return CPU_INTENSIVE;
    }
  };
  protected Processor blockingProcessor = new Processor() {

    @Override
    public Event process(Event event) {
      threads.add(currentThread().getName());
      return event;
    }

    @Override
    public ProcessingType getProccesingType() {
      return BLOCKING;
    }
  };

  protected Scheduler cpuLight;
  protected Scheduler blocking;
  protected Scheduler cpuIntensive;
  private ExecutorService asyncExecutor;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  public AbstractProcessingStrategyTestCase(boolean reactive) {
    super(reactive);
  }

  @Before
  public void before() throws RegistrationException {
    cpuLight = new TestScheduler(2, ThreadType.CPU_LIGHT);
    blocking = new TestScheduler(2, ThreadType.IO);
    cpuIntensive = new TestScheduler(2, ThreadType.CPU_INTENSIVE);
    asyncExecutor = newSingleThreadExecutor();

    flow = new Flow("test", muleContext);
    flow.setProcessingStrategyFactory(muleContext -> createProcessingStrategy(muleContext));
  }

  protected abstract ProcessingStrategy createProcessingStrategy(MuleContext muleContext);

  @After
  public void after() {
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

    assertSingleCpuLight();
  }

  protected abstract void assertSingleCpuLight();

  @Test
  public void singleCpuLightConcurrent() throws Exception {
    FirstInvocationLatchedProcessor latchedProcessor = new FirstInvocationLatchedProcessor(CPU_LITE);

    flow.setMessageProcessors(singletonList(latchedProcessor));
    flow.initialise();
    flow.start();

    asyncExecutor.submit(() -> process(flow, testEvent()));

    latchedProcessor.awaitFirst();
    process(flow, testEvent());
    latchedProcessor.releaseFirst();

    assertSingleCpuLightConcurrent();
  }

  protected abstract void assertSingleCpuLightConcurrent();

  @Test
  public void multipleCpuLight() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuLightProcessor, cpuLightProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());

    assertMultipleCpuLight();
  }

  protected abstract void assertMultipleCpuLight();

  @Test
  public void blocking() throws Exception {
    flow.setMessageProcessors(singletonList(blockingProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());

    assertSingleBlocking();
  }

  protected abstract void assertSingleBlocking();

  @Test
  public void multipleBlocking() throws Exception {
    flow.setMessageProcessors(asList(blockingProcessor, blockingProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());

    assertMultipleBlocking();
  }

  protected abstract void assertMultipleBlocking();

  @Test
  public void singleCpuIntensive() throws Exception {
    flow.setMessageProcessors(singletonList(cpuIntensiveProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());

    assertSingleCpuIntensive();
  }

  protected abstract void assertSingleCpuIntensive();

  @Test
  public void multipleCpuIntensive() throws Exception {
    flow.setMessageProcessors(asList(cpuIntensiveProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());

    assertMultipleCpuIntensive();
  }

  protected abstract void assertMultipleCpuIntensive();

  @Test
  public void mix() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());

    assertMix();
  }

  protected abstract void assertMix();

  @Test
  public void mix2() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuLightProcessor, blockingProcessor, blockingProcessor,
                                     cpuLightProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor, cpuLightProcessor));
    flow.initialise();
    flow.start();

    process(flow, testEvent());

    assertMix2();
  }

  protected abstract void assertMix2();

  @Test
  public abstract void tx() throws Exception;

  class FirstInvocationLatchedProcessor implements Processor {

    private ProcessingType type;
    private volatile Latch latch = new Latch();
    private volatile Latch firstCalledLatch = new Latch();
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
      }

      return event;
    }

    @Override
    public ProcessingType getProccesingType() {
      return type;
    }

    public void releaseFirst() {
      latch.release();
    }

    public void awaitFirst() throws InterruptedException {
      firstCalledLatch.await();
    }

  }

  static class TestScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

    private ThreadType threadType;

    public TestScheduler(int threads, ThreadType threadType) {
      super(threads, new NamedThreadFactory(threadType.name()));
      this.threadType = threadType;
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
    public ThreadType getThreadType() {
      return threadType;
    }

    public String getName() {
      return TestScheduler.class.getSimpleName();
    }

  }
}
