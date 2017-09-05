/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.core.api.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.OVERLOAD_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.probe.PollingProber.DEFAULT_TIMEOUT;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.bubble;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.context.notification.IntegerAction;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotificationListener;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.AnnotatedProcessor;
import org.mule.runtime.core.api.processor.InternalProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

@RunWith(Parameterized.class)
public abstract class AbstractProcessingStrategyTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = getLogger(AbstractProcessingStrategyTestCase.class);
  private static final int CONCURRENT_TEST_CONCURRENCY = 8;

  protected Mode mode;
  protected static final String CPU_LIGHT = "cpuLight";
  protected static final String IO = "I/O";
  protected static final String CPU_INTENSIVE = "cpuIntensive";
  protected static final String CUSTOM = "custom";
  protected static final String RING_BUFFER = "ringBuffer";
  protected static final int STREAM_ITERATIONS = 2000;

  protected Supplier<Builder> flowBuilder;
  protected Flow flow;
  protected Set<String> threads = synchronizedSet(new HashSet<>());
  private TriggerableMessageSource triggerableMessageSource = getTriggerableMessageSource();
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
  protected Processor asyncProcessor = new ThreadTrackingProcessor() {

    @Override
    public ProcessingType getProcessingType() {
      return CPU_LITE_ASYNC;
    }
  };
  protected Processor annotatedAsyncProcessor = new AnnotatedAsyncProcessor();

  protected Processor failingProcessor = new ThreadTrackingProcessor() {

    @Override
    public InternalEvent process(InternalEvent event) {
      throw new RuntimeException("FAILURE");
    }
  };

  protected Processor errorSuccessProcessor = new ThreadTrackingProcessor() {

    private AtomicInteger count = new AtomicInteger();

    @Override
    public InternalEvent process(InternalEvent event) throws MuleException {
      if (count.getAndIncrement() % 10 < 5) {
        return super.process(event);
      } else {
        return failingProcessor.process(event);
      }
    }
  };

  protected Scheduler cpuLight;
  protected Scheduler blocking;
  protected Scheduler cpuIntensive;
  protected Scheduler custom;
  protected Scheduler ringBuffer;
  protected Scheduler asyncExecutor;
  protected ExecutorService cachedThreadPool = newFixedThreadPool(4);



  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  public AbstractProcessingStrategyTestCase(Mode mode) {
    this.mode = mode;
  }

  @Parameterized.Parameters
  public static Collection<Mode> modeParameters() {
    return asList(new Mode[] {Mode.FLOW, SOURCE});
  }

  @Before
  public void before() throws RegistrationException {
    cpuLight = new TestScheduler(2, CPU_LIGHT, false);
    blocking = new TestScheduler(4, IO, true);
    cpuIntensive = new TestScheduler(2, CPU_INTENSIVE, true);
    custom = new TestScheduler(1, CUSTOM, true);
    ringBuffer = new TestScheduler(1, RING_BUFFER, true);
    asyncExecutor = muleContext.getRegistry().lookupObject(SchedulerService.class).ioScheduler();

    flowBuilder = () -> builder("test", muleContext)
        .processingStrategyFactory((muleContext, prefix) -> createProcessingStrategy(muleContext, prefix))
        // Avoid logging of errors by using a null exception handler.
        .messagingExceptionHandler((exception, event) -> event);
  }

  @Override
  protected InternalEvent.Builder getEventBuilder() throws MuleException {
    return InternalEvent
        .builder(create(muleContext.getUniqueIdString(), muleContext.getConfiguration().getId(), TEST_CONNECTOR_LOCATION,
                        NullExceptionHandler.getInstance()));
  }

  protected abstract ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix);

  @After
  public void after() {
    if (flow != null) {
      flow.dispose();
    }
    cpuLight.stop();
    blocking.stop();
    cpuIntensive.stop();
    custom.stop();
    asyncExecutor.stop();
    cachedThreadPool.shutdownNow();
  }

  @Test
  public void singleCpuLight() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(cpuLightProcessor).build();

    flow.initialise();
    flow.start();
    process(flow, testEvent());
  }

  @Test
  public void singleCpuLightConcurrent() throws Exception {
    internalConcurrent(flowBuilder.get(), false, CPU_LITE, 1);
  }

  @Test
  public void singleBlockingConcurrent() throws Exception {
    internalConcurrent(flowBuilder.get(), false, BLOCKING, 1);
  }

  protected void internalConcurrent(Builder flowBuilder, boolean blocks, ProcessingType processingType, int invocations,
                                    Processor... processorsBeforeLatch)
      throws MuleException, InterruptedException {
    MultipleInvocationLatchedProcessor latchedProcessor = new MultipleInvocationLatchedProcessor(processingType, invocations);

    List<Processor> processors = new ArrayList<>(asList(processorsBeforeLatch));
    processors.add(latchedProcessor);
    flow = flowBuilder.source(triggerableMessageSource).processors(processors).build();
    flow.initialise();
    flow.start();

    for (int i = 0; i < invocations; i++) {
      asyncExecutor.submit(() -> process(flow, newEvent()));
    }

    latchedProcessor.getAllLatchedLatch().await();

    asyncExecutor.submit(() -> process(flow, newEvent()));

    assertThat(latchedProcessor.getUnlatchedInvocationLatch().await(BLOCK_TIMEOUT, MILLISECONDS), is(!blocks));

    // We need to assert the threads logged at this point. But good idea to ensure once unlocked the pending invocation completes.
    // To do this need to copy threads locally.
    Set<String> threadsBeforeUnlock = new HashSet<>(threads);

    latchedProcessor.release();

    if (blocks) {
      assertThat(latchedProcessor.getUnlatchedInvocationLatch().await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    }

    threads = threadsBeforeUnlock;
  }

  @Test
  public void multipleCpuLight() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource)
        .processors(cpuLightProcessor, cpuLightProcessor, cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void singleBlocking() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(blockingProcessor).build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void multipleBlocking() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource)
        .processors(blockingProcessor, blockingProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void singleCpuIntensive() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(cpuIntensiveProcessor).build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void multipleCpuIntensive() throws Exception {
    flow =
        flowBuilder.get().source(triggerableMessageSource)
            .processors(cpuIntensiveProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor).build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void mix() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource)
        .processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void mix2() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource)
        .processors(cpuLightProcessor, cpuLightProcessor, blockingProcessor, blockingProcessor,
                    cpuLightProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor, cpuLightProcessor)
        .build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void asyncCpuLight() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(asyncProcessor, cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    process(flow, testEvent());
  }

  @Test
  public void asyncCpuLightConcurrent() throws Exception {
    internalConcurrent(flowBuilder.get(), false, CPU_LITE, 1, asyncProcessor);
  }

  @Test
  public void stream() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch latch = new CountDownLatch(STREAM_ITERATIONS);
    for (int i = 0; i < STREAM_ITERATIONS; i++) {
      switch (mode) {
        case FLOW:
          flow.process(newEvent());
          latch.countDown();
          break;
        case SOURCE:
          processSourceNonBlocking(flow, newEvent(), t -> latch.countDown(),
                                   response -> bubble(new AssertionError("Unexpected error")));
      }
    }
    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void concurrentStream() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch latch = new CountDownLatch(STREAM_ITERATIONS);
    for (int i = 0; i < CONCURRENT_TEST_CONCURRENCY; i++) {
      asyncExecutor.submit(() -> {
        for (int j = 0; j < STREAM_ITERATIONS / CONCURRENT_TEST_CONCURRENCY; j++) {
          try {
            switch (mode) {
              case FLOW:
                flow.process(newEvent());
                latch.countDown();
                break;
              case SOURCE:
                processSourceNonBlocking(flow, newEvent(), t -> latch.countDown(),
                                         response -> bubble(new AssertionError("Unexpected error")));
            }
          } catch (MuleException e) {
            throw new RuntimeException(e);
          }
        }
      });
    }
    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void errorsStream() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(failingProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch latch = new CountDownLatch(STREAM_ITERATIONS);
    for (int i = 0; i < STREAM_ITERATIONS; i++) {
      switch (mode) {
        case FLOW:
          try {
            flow.process(newEvent());
            fail("Unexpected success");
          } catch (Throwable t) {
            latch.countDown();
          }
          break;
        case SOURCE:
          processSourceNonBlocking(flow, newEvent(), response -> bubble(new AssertionError("Unexpected success")),
                                   t -> latch.countDown());
      }
    }
    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void errorSuccessStream() throws Exception {
    flow = flowBuilder.get().source(triggerableMessageSource).processors(errorSuccessProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch sucessLatch = new CountDownLatch(STREAM_ITERATIONS / 2);
    CountDownLatch errorLatch = new CountDownLatch(STREAM_ITERATIONS / 2);
    for (int i = 0; i < STREAM_ITERATIONS; i++) {
      switch (mode) {
        case FLOW:
          try {
            flow.process(newEvent());
            sucessLatch.countDown();
          } catch (Throwable t) {
            errorLatch.countDown();
          }
          break;
        case SOURCE:
          processSourceNonBlocking(flow, newEvent(), response -> sucessLatch.countDown(), t -> errorLatch.countDown());
      }
    }
    assertThat(sucessLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    assertThat(errorLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  protected void processSourceNonBlocking(Flow flow, InternalEvent event, Consumer<InternalEvent> onResponse,
                                          Consumer<Throwable> onError) {
    just(event).transform(triggerableMessageSource.getListener()).subscribe(requestUnbounded());
    from(event.getContext().getResponsePublisher()).subscribe(onResponse, onError);
  }

  @Test
  public abstract void tx() throws Exception;

  protected InternalEvent process(Processor processor, InternalEvent event, boolean unwrapMessagingException) throws Exception {
    setMuleContextIfNeeded(processor, muleContext);
    switch (mode) {
      case FLOW:
        return processor.process(event);
      case SOURCE:
        return triggerableMessageSource.trigger(event);
      default:
        return null;
    }
  }

  protected void testAsyncCpuLightNotificationThreads(AtomicReference<Thread> beforeThread, AtomicReference<Thread> afterThread)
      throws Exception {
    muleContext.getNotificationManager().addInterfaceToType(MessageProcessorNotificationListener.class,
                                                            MessageProcessorNotification.class);
    muleContext.getNotificationManager().addListener((MessageProcessorNotificationListener) notification -> {
      if (new IntegerAction(MESSAGE_PROCESSOR_PRE_INVOKE).equals(notification.getAction())) {
        beforeThread.set(currentThread());
      } else if (new IntegerAction(MESSAGE_PROCESSOR_POST_INVOKE).equals(notification.getAction())) {
        afterThread.set(currentThread());
      }
    });
    flow = flowBuilder.get().processors(annotatedAsyncProcessor).build();
    flow.initialise();
    flow.start();
    process(flow, testEvent());
  }

  protected void testBackPressure(BackPressureStrategy backPressureStrategy, Predicate<Integer> processedAssertion,
                                  Predicate<Integer> rejectedAssertion, Predicate<Integer> totalAssertion)
      throws MuleException {
    if (mode.equals(SOURCE)) {

      TriggerableMessageSource triggerableMessageSource = new TriggerableMessageSource(backPressureStrategy);
      flow =
          flowBuilder.get().source(triggerableMessageSource).processors(asList(cpuLightProcessor, new ThreadTrackingProcessor() {

            @Override
            public InternalEvent process(InternalEvent event) throws MuleException {
              try {
                Thread.sleep(3);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              return super.process(event);
            }

            @Override
            public ProcessingType getProcessingType() {
              return BLOCKING;
            }
          })).maxConcurrency(2).build();
      flow.initialise();
      flow.start();

      AtomicInteger rejected = new AtomicInteger();
      AtomicInteger processed = new AtomicInteger();

      for (int i = 0; i < STREAM_ITERATIONS; i++) {
        cachedThreadPool.submit(() -> Flux.just(newEvent()).transform(triggerableMessageSource.getListener())
            .doOnNext(event -> processed.getAndIncrement())
            .doOnError(e -> rejected.getAndIncrement()).subscribe());
      }

      new PollingProber(DEFAULT_TIMEOUT * 10, DEFAULT_POLLING_INTERVAL)
          .check(new JUnitLambdaProbe(() -> {
            LOGGER.info("DONE " + processed.get() + " , REJECTED " + rejected.get() + ", ");
            return totalAssertion.test(rejected.get() + processed.get())
                && processedAssertion.test(processed.get()) && rejectedAssertion.test(rejected.get());
          }));
    }
  }

  class MultipleInvocationLatchedProcessor implements Processor {

    private ProcessingType type;
    private volatile Latch latch = new Latch();
    private volatile CountDownLatch allLatchedLatch;
    private volatile Latch unlatchedInvocationLatch;
    private AtomicInteger invocations;

    public MultipleInvocationLatchedProcessor(ProcessingType type, int latchedInvocations) {
      this.type = type;
      allLatchedLatch = new CountDownLatch(latchedInvocations);
      unlatchedInvocationLatch = new Latch();
      invocations = new AtomicInteger(latchedInvocations);
    }

    @Override
    public InternalEvent process(InternalEvent event) throws MuleException {
      threads.add(currentThread().getName());
      if (invocations.getAndDecrement() > 0) {
        allLatchedLatch.countDown();
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      } else {
        unlatchedInvocationLatch.countDown();
      }
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return type;
    }

    public void release() {
      latch.release();
    }

    public CountDownLatch getAllLatchedLatch() throws InterruptedException {
      return allLatchedLatch;
    }

    public Latch getUnlatchedInvocationLatch() throws InterruptedException {
      return unlatchedInvocationLatch;
    }

  }

  static class TestScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

    private ExecutorService executor;

    public TestScheduler(int threads, String threadNamePrefix, boolean reject) {
      super(1, new NamedThreadFactory(threadNamePrefix + ".tasks"));
      executor = new ThreadPoolExecutor(threads, threads, 0l, TimeUnit.MILLISECONDS,
                                        new LinkedBlockingQueue(reject ? threads : Integer.MAX_VALUE),
                                        new NamedThreadFactory(threadNamePrefix));
    }

    @Override
    public Future<?> submit(Runnable task) {
      return executor.submit(task);
    }

    @Override
    public void stop() {
      shutdownNow();
      executor.shutdownNow();
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

  /**
   * Scheduler that rejects tasks {@link #REJECTION_COUNT} times and then delegates to delegate scheduler.
   */
  static class RejectingScheduler extends TestScheduler {

    static int REJECTION_COUNT = 10;
    private int rejections;
    private Scheduler delegate;

    public RejectingScheduler(Scheduler delegate) {
      super(1, "prefix", true);
      this.delegate = delegate;
    }

    @Override
    public Future<?> submit(Runnable task) {
      if (rejections++ < REJECTION_COUNT) {
        throw new RejectedExecutionException();
      } else {
        return delegate.submit(task);
      }
    }
  }

  class ThreadTrackingProcessor implements Processor, InternalProcessor {

    @Override
    public InternalEvent process(InternalEvent event) throws MuleException {
      threads.add(currentThread().getName());
      return event;
    }

    @Override
    public Publisher<InternalEvent> apply(Publisher<InternalEvent> publisher) {
      if (getProcessingType() == CPU_LITE_ASYNC) {
        return from(publisher).transform(processorPublisher -> Processor.super.apply(publisher))
            .publishOn(fromExecutorService(custom));
      } else {
        return Processor.super.apply(publisher);
      }
    }

  }

  public static Matcher<Integer> between(int min, int max) {
    return allOf(greaterThanOrEqualTo(min), lessThanOrEqualTo(max));
  }

  public static Matcher<Long> between(long min, long max) {
    return allOf(greaterThanOrEqualTo(min), lessThanOrEqualTo(max));
  }

  protected void expectRejected() {
    expectedException.expect(MessagingException.class);
    expectedException.expect(overloadErrorTypeMatcher());
    expectedException.expectCause(instanceOf(RejectedExecutionException.class));
  }

  private TypeSafeMatcher<MessagingException> overloadErrorTypeMatcher() {
    return new TypeSafeMatcher<MessagingException>() {

      private String errorTypeId;

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendValue(errorTypeId);
      }

      @Override
      protected boolean matchesSafely(MessagingException item) {
        errorTypeId = item.getEvent().getError().get().getErrorType().getIdentifier();
        return OVERLOAD_ERROR_IDENTIFIER.equals(errorTypeId);
      }
    };
  }

  class AnnotatedAsyncProcessor extends AbstractComponent implements AnnotatedProcessor {

    @Override
    public InternalEvent process(InternalEvent event) throws MuleException {
      return asyncProcessor.process(event);
    }

    @Override
    public Publisher<InternalEvent> apply(Publisher<InternalEvent> publisher) {
      return asyncProcessor.apply(publisher);
    }

    @Override
    public ComponentLocation getLocation() {
      return TEST_CONNECTOR_LOCATION;
    }

    @Override
    public ProcessingType getProcessingType() {
      return asyncProcessor.getProcessingType();
    }
  }

  public enum Mode {
    /**
     * Test using {@link Flow#process(InternalEvent)}.
     */
    FLOW,
    /**
     * Test using {@link org.mule.runtime.core.api.source.MessageSource}
     */
    SOURCE,
  }

}
