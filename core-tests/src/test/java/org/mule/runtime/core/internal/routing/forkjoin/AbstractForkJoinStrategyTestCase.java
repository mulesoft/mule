/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.forkjoin;


import static java.lang.Integer.MAX_VALUE;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.TIMEOUT;
import static org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair.of;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.test.allure.AllureConstants.ForkJoinStrategiesFeature.FORK_JOIN_STRATEGIES;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.routing.CompositeRoutingException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair;
import org.mule.runtime.core.privileged.routing.RoutingResult;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;

@Feature(FORK_JOIN_STRATEGIES)
public abstract class AbstractForkJoinStrategyTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected ForkJoinStrategy strategy;
  protected ProcessingStrategy processingStrategy;
  protected Scheduler scheduler;
  protected ErrorType timeoutErrorType;

  @Before
  public void setup() {
    processingStrategy = mock(ProcessingStrategy.class);
    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, ReactiveProcessor.class));
    scheduler = muleContext.getSchedulerService().ioScheduler();
    timeoutErrorType = muleContext.getErrorTypeRepository().getErrorType(TIMEOUT).get();
    setupConcurrentProcessingStrategy();
    strategy = createStrategy(processingStrategy, Integer.MAX_VALUE, true, MAX_VALUE);
  }

  @After
  public void tearDown() {
    scheduler.stop();
  }

  protected abstract ForkJoinStrategy createStrategy(ProcessingStrategy processingStrategy, int concurrency, boolean delayErrors,
                                                     long timeout);

  @Test
  @Description("When a route timeout occurs a CompositeRoutingException is thrown with details of timeout error in RoutingResult.")
  public void timeout() throws Throwable {
    strategy = createStrategy(processingStrategy, 1, true, 50);

    expectedException.expect(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(strategy, testEvent(), asList(createRoutingPairWithSleep(of(1), 250)), throwable -> {
      CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 1);
      RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 0, 1);
      assertThat(routingResult.getFailures().get("0").getCause(), instanceOf(TimeoutException.class));
    });
  }

  @Test
  @Description("When a route timeout occurs all routes are still executed and  a CompositeRoutingException is thrown with details of timeout error and successful routes in RoutingResult.")
  public void timeoutDelayed() throws Throwable {
    strategy = createStrategy(processingStrategy, 1, true, 50);

    Message pair2Result = of(2);
    Processor pair2Processor = createProcessorSpy(pair2Result);
    RoutingPair pair2 = of(testEvent(), createChain(pair2Processor));

    expectedException.expect(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(strategy, testEvent(), asList(createRoutingPairWithSleep(of(1), 250), pair2),
                           throwable -> {
                             verify(pair2Processor, times(1)).process(any(CoreEvent.class));
                             CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 1);
                             RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 1, 1);
                             assertThat(routingResult.getFailures().get("0").getCause(),
                                        instanceOf(TimeoutException.class));
                             assertThat(routingResult.getResults().get("1"), is(pair2Result));
                           });
  }

  @Test
  @Description("When configured with delayErrors='false' the first timeout causes strategy to throw a TimeoutException.")
  public void timeoutEager() throws Throwable {
    strategy = createStrategy(processingStrategy, 1, false, 50);

    Message pair2Result = of(2);
    Processor pair2Processor = createProcessorSpy(pair2Result);
    RoutingPair pair2 = of(testEvent(), createChain(pair2Processor));

    expectedException.expect(instanceOf(DefaultMuleException.class));
    expectedException.expectCause(instanceOf(TimeoutException.class));

    invokeStrategyBlocking(strategy, testEvent(),
                           asList(createRoutingPairWithSleep(of(1), 250), pair2),
                           throwable -> verify(pair2Processor, never()).process(any(CoreEvent.class)));
  }

  @Test
  @Description("Errors are thrown via CompositeRoutingException with RoutingResult containing details of failures.")
  public void error() throws Throwable {
    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(testEvent(), createFailingRoutingPair(exception));

    expectedException.expect(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(strategy, testEvent(), asList(failingPair), throwable -> {
      CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 1);
      RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 0, 1);
      assertThat(routingResult.getFailures().get("0").getCause(), is(exception));
    });
  }

  @Test
  @Description("When an error occurs all routes are executed regardless and a CompositeRoutingException is thrown containing a RoutingResult with details of both failures and successes.")
  public void errorDelayed() throws Throwable {
    Processor processorSpy = createProcessorSpy(testEvent().getMessage());

    RuntimeException exception1 = new IllegalStateException();
    RoutingPair failingPair1 = of(testEvent(), createFailingRoutingPair(exception1));
    RuntimeException exception2 = new UnsupportedOperationException();
    RoutingPair failingPair2 = of(testEvent(), createFailingRoutingPair(exception2));
    RuntimeException exception3 = new IndexOutOfBoundsException();
    RoutingPair failingPair3 = of(testEvent(), createFailingRoutingPair(exception3));
    RoutingPair okPair = of(testEvent(), createChain(processorSpy));

    expectedException.expect(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(strategy, testEvent(), asList(failingPair1, failingPair2, failingPair3, okPair), throwable -> {
      verify(processorSpy, times(1)).process(any(CoreEvent.class));
      CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 3);
      RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 1, 3);
      assertThat(routingResult.getFailures().get("0").getCause(), is(exception1));
      assertThat(routingResult.getFailures().get("1").getCause(), is(exception2));
      assertThat(routingResult.getFailures().get("2").getCause(), is(exception3));
      assertThat(routingResult.getFailures().get("3"), is(nullValue()));
    });
  }

  @Test
  @Description("When configured with delayErrors='false' the first errors causes strategy to throw this exception.")
  public void errorEager() throws Throwable {
    strategy = createStrategy(processingStrategy, 1, false, MAX_VALUE);

    Processor processorSpy = createProcessorSpy(of(1));

    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(testEvent(), createFailingRoutingPair(exception));
    RoutingPair okPair = of(testEvent(), createChain(processorSpy));

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(is(exception));

    invokeStrategyBlocking(strategy, testEvent(), asList(failingPair, okPair),
                           throwable -> verify(processorSpy, never()).process(any(CoreEvent.class)));
  }

  @Test
  @Description("When configured with delayErrors='false' the first errors causes strategy to throw this exception. Other routes may or may not be executed depending on concurrency.")
  public void errorEagerConcurrent() throws Throwable {
    strategy = createStrategy(processingStrategy, 4, false, MAX_VALUE);
    Processor processorSpy = createProcessorSpy(of(1));
    Processor processorSpy2 = createProcessorSpy(of(2));
    Processor processorSpy3 = createProcessorSpy(of(3));

    CoreEvent orignial = testEvent();
    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(orignial, createFailingRoutingPair(exception));
    RoutingPair okPair = of(orignial, createChain(processorSpy));
    RoutingPair okPair2 = of(orignial, createChain(processorSpy2));
    RoutingPair okPair3 = of(orignial, createChain(processorSpy3));

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(is(exception));


    invokeStrategyBlocking(strategy, testEvent(), asList(failingPair, okPair, okPair2, okPair3), throwable -> {
      verify(processorSpy, atMost(1)).process(any(CoreEvent.class));
      verify(processorSpy2, atMost(1)).process(any(CoreEvent.class));
      verify(processorSpy3, atMost(1)).process(any(CoreEvent.class));
    });
  }

  @Test
  @Description("After successful completion of all routes the variables from each route are merged into the result.")
  public void flowVarsMerged() throws Throwable {
    final String beforeVarName = "before";
    final String beforeVarValue = "beforeValue";
    final String beforeVar2Name = "before2";
    final String beforeVar2Value = "before2Value";
    final String beforeVar2NewValue = "before2NewValue";
    final String fooVarName = "foo";
    final String fooVarValue = "fooValue1";
    final String fooVar2Name = "foo2";
    final String fooVar2Value1 = "foo2Value1";
    final String fooVar2Value2 = "foo2Value2";
    final String fooVar3Name = "foo3";
    final String fooVar3Value1 = "foo3Value1";
    final Apple fooVar3Value2 = new Apple();

    CoreEvent original = builder(this.<CoreEvent>newEvent())
        .addVariable(beforeVarName, beforeVarValue)
        .addVariable(beforeVar2Name, beforeVar2Value)
        .build();

    RoutingPair pair1 = of(original, createChain(event -> builder(event)
        .addVariable(beforeVar2Name, beforeVar2NewValue)
        .addVariable(fooVarName, fooVarValue)
        .addVariable(fooVar2Name, fooVar2Value1)
        .addVariable(fooVar3Name, fooVar3Value1)
        .build()));
    RoutingPair pair2 = of(original, createChain(event -> builder(event)
        .addVariable(fooVar2Name, fooVar2Value2)
        .addVariable(fooVar3Name, fooVar3Value2)
        .build()));

    CoreEvent result = invokeStrategyBlocking(strategy, original, asList(pair1, pair2));

    assertThat(result.getVariables().keySet(), hasSize(5));

    assertThat(result.getVariables().keySet(),
               hasItems(beforeVarName, beforeVar2Name, fooVarName, fooVarName, fooVar2Name, fooVar3Name));

    assertThat(result.getVariables().get(beforeVarName).getValue(), equalTo(beforeVarValue));
    assertThat(result.getVariables().get(beforeVar2Name).getValue(), equalTo(beforeVar2NewValue));

    assertThat(result.getVariables().get(fooVarName).getValue(), equalTo(fooVarValue));

    TypedValue fooVar2 = result.getVariables().get(fooVar2Name);
    assertThat(fooVar2.getDataType(), equalTo(DataType.builder().collectionType(List.class).itemType(String.class).build()));
    assertThat(((List<String>) fooVar2.getValue()), hasItems(fooVar2Value1, fooVar2Value2));

    TypedValue fooVar3 = result.getVariables().get(fooVar3Name);
    assertThat(fooVar3.getDataType(), equalTo(DataType.builder().collectionType(List.class).itemType(Object.class).build()));
    assertThat(((List<Object>) fooVar3.getValue()), hasItems(fooVar3Value1, fooVar3Value2));
  }

  @Test
  @Description("When the strategy uses a processing strategy that supports concurrent execution the total processing time is less that sequential processing.")
  public void concurrent() throws Throwable {
    int pairs = 10;
    int processorSleep = 50;
    invokeStrategyBlocking(strategy, testEvent(), createRoutingPairs(pairs, processorSleep));

    verify(scheduler, times(pairs)).submit(any(Callable.class));
  }

  @Test
  @Description("When executing concurrently the strategy will throw a RejectedExceptionException if the scheduler being used throws a RejectedExceptionException.")
  public void concurrentRejectedExecution() throws Throwable {
    when(scheduler.submit(any(Callable.class))).thenThrow(new RejectedExecutionException());
    setupConcurrentProcessingStrategy();
    strategy = createStrategy(processingStrategy, 4, true, MAX_VALUE);

    expectedException.expect(RejectedExecutionException.class);
    invokeStrategyBlocking(strategy, testEvent(), createRoutingPairs(1));
  }

  @Test
  @Description("When concurrency is limited to '1' routes execute sequentially and the total processing time is the sum of processing each route, regardless of if the processing strategy supports concurrency.")
  public void sequential() throws Throwable {
    setupConcurrentProcessingStrategy();
    strategy = createStrategy(processingStrategy, 1, true, MAX_VALUE);

    int pairs = 10;
    int processorSleep = 50;
    invokeStrategyBlocking(strategy, testEvent(), createRoutingPairs(pairs, processorSleep));

    verify(scheduler, never()).submit(any(Runnable.class));
  }

  private void setupConcurrentProcessingStrategy() {
    Function<ReactiveProcessor, ReactiveProcessor> scheduleFunction =
        processor -> publisher -> from(publisher).publishOn(fromExecutorService(scheduler)).transform(processor);
    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenAnswer(invocation -> scheduleFunction.apply(invocation.getArgumentAt(0, ReactiveProcessor.class)));
  }

  private CompositeRoutingException assertCompositeRoutingException(Throwable throwable, int errors) {
    assertThat(throwable, instanceOf(CompositeRoutingException.class));
    CompositeRoutingException compositeRoutingException = (CompositeRoutingException) throwable;
    assertThat(compositeRoutingException.getErrors().size(), is(errors));
    return compositeRoutingException;
  }

  private RoutingResult assertRoutingResult(CompositeRoutingException compositeRoutingException, int results, int errors) {
    assertThat(compositeRoutingException.getErrorMessage().getPayload().getValue(), instanceOf(RoutingResult.class));
    RoutingResult routingResult = (RoutingResult) compositeRoutingException.getErrorMessage().getPayload().getValue();
    assertThat(routingResult.getResults().size(), is(results));
    assertThat(routingResult.getFailures().size(), is(errors));
    return routingResult;
  }

  protected CoreEvent invokeStrategyBlocking(ForkJoinStrategy strategy, CoreEvent original,
                                             List<RoutingPair> routingPairs)
      throws Throwable {
    return invokeStrategyBlocking(strategy, original, routingPairs, throwable -> {
    });
  }

  protected CoreEvent invokeStrategyBlocking(ForkJoinStrategy strategy, CoreEvent original,
                                             List<RoutingPair> routingPairs,
                                             CheckedConsumer<Throwable> verifyOnError)
      throws Throwable {
    try {
      return from(strategy.forkJoin(original, fromIterable(routingPairs))).block();
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);
      verifyOnError.accept(throwable);
      throw throwable;
    }
  }

  private MessageProcessorChain createFailingRoutingPair(RuntimeException exception) throws MuleException {
    return createChain((InternalTestProcessor) event -> {
      throw exception;
    });
  }

  protected Processor createProcessorSpy(Message result) throws MuleException {
    // Mockito does not support lambda
    return spy(new InternalTestProcessor() {

      @Override
      public CoreEvent process(CoreEvent event) throws MuleException {
        return CoreEvent.builder(event).message(result).build();
      }
    });
  }

  protected RoutingPair createRoutingPair(Processor processor) throws MuleException {
    return of(testEvent(), createChain(processor));
  }

  protected RoutingPair createRoutingPair(Message result) throws MuleException {
    return createRoutingPairWithSleep(result, 0);
  }

  private RoutingPair createRoutingPairWithSleep(Message result, long sleep) throws MuleException {
    return of(testEvent(), createChain(event -> {
      try {
        sleep(sleep);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return CoreEvent.builder(event).message(result).build();
    }));
  }

  private List<RoutingPair> createRoutingPairs(int number) {
    return createRoutingPairs(number, 0);
  }

  private List<RoutingPair> createRoutingPairs(int number, int sleep) {
    return range(0, number).mapToObj(i -> {
      try {
        return createRoutingPairWithSleep(of(1), sleep);
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    }).collect(toList());
  }

  private MessageProcessorChain createChain(Processor processor) throws MuleException {
    MessageProcessorChain chain = newChain(Optional.empty(), processor);
    chain.setMuleContext(muleContext);
    return chain;
  }

  @FunctionalInterface
  private interface InternalTestProcessor extends Processor, InternalProcessor {

  }
}
