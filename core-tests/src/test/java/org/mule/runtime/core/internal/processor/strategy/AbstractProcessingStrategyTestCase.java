/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.FLOW_BACK_PRESSURE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.PROCESSOR_SCHEDULER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.probe.PollingProber.DEFAULT_TIMEOUT;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.bubble;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;
import org.mule.runtime.core.internal.construct.FlowBackPressureException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
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
import java.util.function.Supplier;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
  protected Set<String> schedulers = synchronizedSet(new HashSet<>());
  protected TriggerableMessageSource triggerableMessageSource = getTriggerableMessageSource();
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
    public CoreEvent process(CoreEvent event) {
      throw new RuntimeException("FAILURE");
    }
  };

  protected Processor errorSuccessProcessor = new ThreadTrackingProcessor() {

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      if (count.getAndIncrement() % 10 < 5) {
        return super.process(event);
      } else {
        return failingProcessor.process(event);
      }
    }
  };

  protected Processor ioRWProcessor = new ThreadTrackingProcessor() {

    @Override
    public ProcessingType getProcessingType() {
      return IO_RW;
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

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Mode> modeParameters() {
    return asList(new Mode[] {Mode.FLOW, SOURCE});
  }

  @Before
  public void before() throws RegistrationException {
    cpuLight = new TestScheduler(2, CPU_LIGHT, false);
    blocking = new TestScheduler(4, IO, true);
    cpuIntensive = new TestScheduler(2, CPU_INTENSIVE, true);
    custom = new TestScheduler(4, CUSTOM, true);
    ringBuffer = new TestScheduler(1, RING_BUFFER, true);
    asyncExecutor = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(SchedulerService.class).ioScheduler();

    flowBuilder = () -> builder("test", muleContext)
        .processingStrategyFactory((muleContext, prefix) -> createProcessingStrategy(muleContext, prefix))
        .source(triggerableMessageSource)
        // Avoid logging of errors by using a null exception handler.
        .messagingExceptionHandler((exception, event) -> event);
  }

  @Override
  protected InternalEvent.Builder getEventBuilder() throws MuleException {
    return InternalEvent.builder(create(flow, TEST_CONNECTOR_LOCATION));
  }

  protected abstract ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix);

  @After
  public void after() throws MuleException {
    if (flow != null) {
      flow.stop();
      flow.dispose();
    }
    ringBuffer.stop();
    cpuLight.stop();
    blocking.stop();
    cpuIntensive.stop();
    custom.stop();
    asyncExecutor.stop();
    cachedThreadPool.shutdownNow();
  }

  @Test
  public void singleCpuLight() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor).build();

    flow.initialise();
    flow.start();
    processFlow(testEvent());

    assertThat(schedulers, cpuLightSchedulerMatcher());
  }

  protected Matcher<Iterable<? extends String>> cpuLightSchedulerMatcher() {
    return contains(CPU_LIGHT);
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
    flow = flowBuilder.processors(processors).build();
    flow.initialise();
    flow.start();

    for (int i = 0; i < invocations; i++) {
      asyncExecutor.submit(() -> processFlow(newEvent()));
    }

    latchedProcessor.getAllLatchedLatch().await();

    asyncExecutor.submit(() -> processFlow(newEvent()));

    assertThat(latchedProcessor.getUnlatchedInvocationLatch().await(BLOCK_TIMEOUT, MILLISECONDS), is(!blocks));

    // We need to assert the threads logged at this point. But good idea to ensure once unlocked the pending invocation completes.
    // To do this need to copy threads locally.
    Set<String> threadsBeforeUnlock = new HashSet<>(threads);
    Set<String> schedulersBeforeUnlock = new HashSet<>(schedulers);

    latchedProcessor.release();

    if (blocks) {
      assertThat(latchedProcessor.getUnlatchedInvocationLatch().await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    }

    threads = threadsBeforeUnlock;
    schedulers = schedulersBeforeUnlock;
  }

  @Test
  public void multipleCpuLight() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuLightProcessor, cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());
  }

  @Test
  public void singleBlocking() throws Exception {
    flow = flowBuilder.get().processors(blockingProcessor).build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());

    assertThat(schedulers, ioSchedulerMatcher());
  }

  protected Matcher<Iterable<? extends String>> ioSchedulerMatcher() {
    return contains(IO);
  }

  @Test
  public void multipleBlocking() throws Exception {
    flow = flowBuilder.get().processors(blockingProcessor, blockingProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());
  }

  @Test
  public void singleCpuIntensive() throws Exception {
    flow = flowBuilder.get().processors(cpuIntensiveProcessor).build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());

    assertThat(schedulers, cpuIntensiveSchedulerMatcher());
  }

  protected Matcher<Iterable<? extends String>> cpuIntensiveSchedulerMatcher() {
    return contains(CPU_INTENSIVE);
  }

  @Test
  public void multipleCpuIntensive() throws Exception {
    flow =
        flowBuilder.get().processors(cpuIntensiveProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor).build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());
  }

  @Test
  public void mix() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());
  }

  @Test
  public void mix2() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuLightProcessor, blockingProcessor, blockingProcessor,
                                        cpuLightProcessor, cpuIntensiveProcessor, cpuIntensiveProcessor, cpuLightProcessor)
        .build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());
  }

  @Test
  public void asyncCpuLight() throws Exception {
    flow = flowBuilder.get().processors(asyncProcessor, cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());
  }

  @Test
  public void asyncCpuLightConcurrent() throws Exception {
    internalConcurrent(flowBuilder.get(), false, CPU_LITE, 1, asyncProcessor);
  }

  @Test
  public void stream() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch latch = new CountDownLatch(STREAM_ITERATIONS);
    for (int i = 0; i < STREAM_ITERATIONS; i++) {
      dispatchFlow(newEvent(), t -> latch.countDown(), response -> bubble(new AssertionError("Unexpected error")));
    }
    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void concurrentStream() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch latch = new CountDownLatch(STREAM_ITERATIONS);
    for (int i = 0; i < CONCURRENT_TEST_CONCURRENCY; i++) {
      asyncExecutor.submit(() -> {
        for (int j = 0; j < STREAM_ITERATIONS / CONCURRENT_TEST_CONCURRENCY; j++) {
          try {
            dispatchFlow(newEvent(), t -> latch.countDown(), response -> bubble(new AssertionError("Unexpected error")));
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
    flow = flowBuilder.get().processors(failingProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch latch = new CountDownLatch(STREAM_ITERATIONS);
    for (int i = 0; i < STREAM_ITERATIONS; i++) {
      dispatchFlow(newEvent(), response -> bubble(new AssertionError("Unexpected success")), t -> latch.countDown());
    }
    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void errorSuccessStream() throws Exception {
    flow = flowBuilder.get().processors(errorSuccessProcessor).build();
    flow.initialise();
    flow.start();

    CountDownLatch sucessLatch = new CountDownLatch(STREAM_ITERATIONS / 2);
    CountDownLatch errorLatch = new CountDownLatch(STREAM_ITERATIONS / 2);
    for (int i = 0; i < STREAM_ITERATIONS; i++) {
      dispatchFlow(newEvent(), response -> sucessLatch.countDown(), t -> errorLatch.countDown());
    }
    assertThat(sucessLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    assertThat(errorLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public abstract void tx() throws Exception;

  protected void singleIORW(Callable<CoreEvent> eventSupplier, Matcher<Iterable<? extends String>> schedulerNameMatcher)
      throws Exception {
    flow = flowBuilder.get().processors(ioRWProcessor).build();

    flow.initialise();
    flow.start();
    processFlow(eventSupplier.call());
    assertThat(schedulers, schedulerNameMatcher);
  }

  protected CoreEvent processFlow(CoreEvent event) throws Exception {
    setMuleContextIfNeeded(flow, muleContext);
    switch (mode) {
      case FLOW:
        return flow.process(event);
      case SOURCE:
        Publisher<CoreEvent> responsePublisher = ((BaseEventContext) event.getContext()).getResponsePublisher();
        just(event)
            .transform(triggerableMessageSource.getListener())
            .subscribe(requestUnbounded());
        try {
          return Mono.from(responsePublisher).block();
        } catch (Throwable throwable) {
          throw rxExceptionToMuleException(throwable);
        }
      default:
        return null;
    }
  }

  protected void dispatchFlow(CoreEvent event, Consumer<CoreEvent> onSuccess,
                              Consumer<Throwable> onError) {
    setMuleContextIfNeeded(flow, muleContext);
    switch (mode) {
      case FLOW:
        ((BaseEventContext) event.getContext()).onResponse((response, throwable) -> {
          onSuccess.accept(response);
          onError.accept(throwable);
        });
        just(event).transform(flow).subscribe(requestUnbounded());
        break;
      case SOURCE:
        ((BaseEventContext) event.getContext()).onResponse((response, throwable) -> {
          onSuccess.accept(response);
          onError.accept(throwable);
        });
        just(event).transform(triggerableMessageSource.getListener()).subscribe(requestUnbounded());
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
    processFlow(testEvent());
  }

  protected void testBackPressure(BackPressureStrategy backPressureStrategy, Matcher<Integer> processedAssertion,
                                  Matcher<Integer> rejectedAssertion, Matcher<Integer> totalAssertion)
      throws MuleException {
    if (mode.equals(SOURCE)) {

      triggerableMessageSource = new TriggerableMessageSource(backPressureStrategy);
      flow =
          flowBuilder.get()
              .source(triggerableMessageSource)
              .processors(asList(cpuLightProcessor, new ThreadTrackingProcessor() {

                @Override
                public CoreEvent process(CoreEvent event) throws MuleException {
                  try {
                    sleep(3);
                  } catch (InterruptedException e) {
                    currentThread().interrupt();
                    throw new RuntimeException(e);
                  }
                  return super.process(event);
                }

                @Override
                public ProcessingType getProcessingType() {
                  return BLOCKING;
                }
              }))
              .maxConcurrency(2)
              .build();
      flow.initialise();
      flow.start();

      AtomicInteger rejected = new AtomicInteger();
      AtomicInteger processed = new AtomicInteger();

      for (int i = 0; i < STREAM_ITERATIONS; i++) {
        cachedThreadPool.submit(() -> Flux.just(newEvent())
            .cast(CoreEvent.class).transform(triggerableMessageSource.getListener())
            .doOnNext(event -> processed.getAndIncrement())
            .doOnError(e -> rejected.getAndIncrement()).subscribe());
      }

      new PollingProber(DEFAULT_TIMEOUT * 10, DEFAULT_POLLING_INTERVAL)
          .check(new JUnitLambdaProbe(() -> {
            LOGGER.info("DONE " + processed.get() + " , REJECTED " + rejected.get() + ", ");

            assertThat("total", rejected.get() + processed.get(), totalAssertion);
            assertThat("processed", processed.get(), processedAssertion);
            assertThat("rejected", rejected.get(), rejectedAssertion);

            return true;
          }));
    }
  }

  class MultipleInvocationLatchedProcessor extends AbstractComponent implements Processor {

    private final ProcessingType type;
    private volatile Latch latch = new Latch();
    private volatile CountDownLatch allLatchedLatch;
    private volatile Latch unlatchedInvocationLatch;
    private final AtomicInteger invocations;

    public MultipleInvocationLatchedProcessor(ProcessingType type, int latchedInvocations) {
      this.type = type;
      allLatchedLatch = new CountDownLatch(latchedInvocations);
      unlatchedInvocationLatch = new Latch();
      invocations = new AtomicInteger(latchedInvocations);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
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

    private final String threadNamePrefix;
    private final ExecutorService executor;

    public TestScheduler(int threads, String threadNamePrefix, boolean reject) {
      super(1, new NamedThreadFactory(threadNamePrefix + ".tasks"));
      this.threadNamePrefix = threadNamePrefix;
      executor = new ThreadPoolExecutor(threads, threads, 0l, TimeUnit.MILLISECONDS,
                                        new LinkedBlockingQueue(reject ? threads : Integer.MAX_VALUE),
                                        new NamedThreadFactory(threadNamePrefix));
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

  /**
   * Scheduler that rejects tasks {@link #REJECTION_COUNT} times and then delegates to delegate scheduler.
   */
  static class RejectingScheduler extends TestScheduler {

    static int REJECTION_COUNT = 10;
    private int rejections;
    private final Scheduler delegate;

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

    @Override
    public Future<?> submit(Callable task) {
      if (rejections++ < REJECTION_COUNT) {
        throw new RejectedExecutionException();
      } else {
        return delegate.submit(task);
      }
    }
  }

  class ThreadTrackingProcessor implements Processor, InternalProcessor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      threads.add(currentThread().getName());
      return event;
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      Flux<CoreEvent> schedulerTrackingPublisher = from(publisher)
          .doOnEach(signal -> signal.getContext().getOrEmpty(PROCESSOR_SCHEDULER_CONTEXT_KEY)
              .ifPresent(sch -> schedulers.add(((Scheduler) sch).getName())));

      if (getProcessingType() == CPU_LITE_ASYNC) {
        return from(schedulerTrackingPublisher).transform(processorPublisher -> Processor.super.apply(schedulerTrackingPublisher))
            .publishOn(fromExecutorService(custom)).errorStrategyStop();
      } else {
        return Processor.super.apply(schedulerTrackingPublisher);
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
    expectedException.expectCause(instanceOf(FlowBackPressureException.class));
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
        return FLOW_BACK_PRESSURE_ERROR_IDENTIFIER.equals(errorTypeId);
      }
    };
  }

  class AnnotatedAsyncProcessor extends AbstractComponent implements AnnotatedProcessor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return asyncProcessor.process(event);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
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
     * Test using {@link Flow#process(CoreEvent)}.
     */
    FLOW,
    /**
     * Test using {@link org.mule.runtime.core.api.source.MessageSource}
     */
    SOURCE
  }
}
