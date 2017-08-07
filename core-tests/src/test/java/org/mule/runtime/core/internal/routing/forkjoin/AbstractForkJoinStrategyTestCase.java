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
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair.of;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair;
import org.mule.runtime.core.internal.routing.CompositeRoutingException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public abstract class AbstractForkJoinStrategyTestCase extends AbstractMuleContextTestCase {

  protected ForkJoinStrategy strategy;
  protected ProcessingStrategy processingStrategy;
  private Scheduler ioScheduler;

  @Before
  public void setup() {
    processingStrategy = mock(ProcessingStrategy.class);

    Function<ReactiveProcessor, ReactiveProcessor> scheduleFunction =
        processor -> publisher -> from(publisher).publishOn(fromExecutorService(ioScheduler)).transform(processor);
    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenAnswer(invocation -> scheduleFunction.apply(invocation.getArgumentAt(0, ReactiveProcessor.class)));
    strategy = createStrategy(Long.MAX_VALUE);
    ioScheduler = muleContext.getSchedulerService().ioScheduler();
  }

  @After
  public void tearown() {
    ioScheduler.stop();
  }

  protected abstract ForkJoinStrategy createStrategy(long timeout);

  @Test
  public void timeout() throws Throwable {

    strategy = createStrategy(50);

    Event original = testEvent();
    Event route1Result = newEvent();
    Event route2Result = newEvent();

    RoutingPair timeoutPair = createSleepingRoutingPair(route1Result, 100);
    RoutingPair otherPair = of(testEvent(), event -> route2Result);

    try {
      from(strategy.forkJoin(original, fromIterable(asList(timeoutPair, otherPair)), processingStrategy, 2, true)).block();
      fail("Expected exception");
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);
      assertThat(throwable, instanceOf(MessagingException.class));
      assertThat(((MessagingException) throwable).getRootCause(), instanceOf(TimeoutException.class));
    }
  }

  @Test
  public void error() throws Throwable {
    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(testEvent(), createFailingRoutingPair(exception));

    try {
      from(strategy.forkJoin(testEvent(), fromIterable(asList(failingPair)), processingStrategy, 2, true)).block();
      fail("Expected exception");
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);

      assertThat(throwable, instanceOf(MessagingException.class));
      assertThat(throwable.getCause(), instanceOf(CompositeRoutingException.class));

      CompositeRoutingException compositeRoutingException = (CompositeRoutingException) throwable.getCause();
      assertThat(compositeRoutingException.getExceptions().size(), is(1));
      assertThat(compositeRoutingException.getExceptionForRouteIndex(0), is(exception));
    }
  }

  @Test
  public void errorDelayed() throws Throwable {

    Processor processorSpy = createEchoProcessorSpy();

    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(testEvent(), createFailingRoutingPair(exception));
    RoutingPair okPair = of(testEvent(), processorSpy);


    try {
      from(strategy.forkJoin(testEvent(), fromIterable(asList(failingPair, okPair)), processingStrategy, 2, true)).block();
      fail("Expected exception");
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);

      verify(processorSpy, times(1)).process(any(Event.class));

      assertThat(throwable, instanceOf(MessagingException.class));
      assertThat(throwable.getCause(), instanceOf(CompositeRoutingException.class));

      CompositeRoutingException compositeRoutingException = (CompositeRoutingException) throwable.getCause();
      assertThat(compositeRoutingException.getExceptions().size(), is(1));
      assertThat(compositeRoutingException.getExceptionForRouteIndex(0), is(exception));
      assertThat(compositeRoutingException.getExceptionForRouteIndex(1), nullValue());
    }
  }

  @Test
  public void errorEager() throws Throwable {

    Processor processorSpy = createEchoProcessorSpy();

    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(testEvent(), createFailingRoutingPair(exception));
    RoutingPair okPair = of(testEvent(), processorSpy);


    try {
      from(strategy.forkJoin(testEvent(), fromIterable(asList(failingPair, okPair)), processingStrategy, 1, false)).block();
      fail("Expected exception");
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);

      verify(processorSpy, never()).process(any(Event.class));

      assertThat(throwable, instanceOf(MessagingException.class));
      assertThat(throwable.getCause(), is(exception));
    }
  }

  @Test
  public void errorEagerConcurrent() throws Throwable {
    Processor processorSpy = createEchoProcessorSpy();
    Processor processorSpy2 = createEchoProcessorSpy();
    Processor processorSpy3 = createEchoProcessorSpy();

    Event orignial = testEvent();
    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(orignial, createFailingRoutingPair(exception));
    RoutingPair okPair = of(orignial, processorSpy);
    RoutingPair okPair2 = of(orignial, processorSpy2);
    RoutingPair okPair3 = of(orignial, processorSpy3);


    try {
      from(strategy.forkJoin(orignial, fromIterable(asList(failingPair, okPair, okPair2, okPair3)), processingStrategy,
                             4, false)).block();
      fail("Expected exception");
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);

      verify(processorSpy, atMost(1)).process(any(Event.class));
      verify(processorSpy2, atMost(1)).process(any(Event.class));
      verify(processorSpy3, atMost(1)).process(any(Event.class));

      assertThat(throwable, instanceOf(MessagingException.class));
      assertThat(throwable.getCause(), is(exception));
    }
  }

  @Test
  public void mutipleErrors() throws Throwable {

    Processor processorSpy = createEchoProcessorSpy();

    RuntimeException exception1 = new IllegalStateException();
    RoutingPair failingPair1 = of(testEvent(), createFailingRoutingPair(exception1));
    RuntimeException exception2 = new UnsupportedOperationException();
    RoutingPair failingPair2 = of(testEvent(), createFailingRoutingPair(exception2));
    RuntimeException exception3 = new IndexOutOfBoundsException();
    RoutingPair failingPair3 = of(testEvent(), createFailingRoutingPair(exception3));
    RoutingPair okPair = of(testEvent(), processorSpy);


    try {
      from(strategy.forkJoin(testEvent(), fromIterable(asList(failingPair1, failingPair2, failingPair3, okPair)),
                             processingStrategy, 4, true))
                                 .block();
      fail("Expected exception");
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);

      assertThat(throwable, instanceOf(MessagingException.class));
      assertThat(throwable.getCause(), instanceOf(CompositeRoutingException.class));

      CompositeRoutingException compositeRoutingException = (CompositeRoutingException) throwable.getCause();
      assertThat(compositeRoutingException.getExceptions().size(), is(3));
      assertThat(compositeRoutingException.getExceptionForRouteIndex(0), is(exception1));
      assertThat(compositeRoutingException.getExceptionForRouteIndex(1), is(exception2));
      assertThat(compositeRoutingException.getExceptionForRouteIndex(2), is(exception3));
      assertThat(compositeRoutingException.getExceptionForRouteIndex(3), nullValue());
    }
  }

  @Test
  public void flowVarsMerged() throws Exception {
    String beforeVarName = "before";
    String fooVarName = "foo";
    String foo2VarName = "foo2";

    Event original = builder(newEvent()).addVariable(beforeVarName, "value").build();

    RoutingPair pair1 = of(testEvent(), event -> builder(event).addVariable(fooVarName, "bar").build());
    RoutingPair pair2 = of(testEvent(), event -> builder(event).addVariable(foo2VarName, "bar2").build());

    Event result =
        from(strategy.forkJoin(original, fromIterable(asList(pair1, pair2)), processingStrategy, 2, true)).block();

    assertThat(result.getVariables().keySet(), hasSize(3));
    assertThat(result.getVariables().keySet(), hasItems(beforeVarName, fooVarName, foo2VarName));
  }

  @Test
  public void concurrent() throws Throwable {

    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenReturn(publisher -> from(publisher).publishOn(fromExecutorService(ioScheduler)));

    int pairs = 10;
    from(strategy.forkJoin(testEvent(), fromIterable(getRoutingPairs(pairs)), processingStrategy, MAX_VALUE, true)).block();

    verify(ioScheduler, times(pairs)).submit(any(Runnable.class));
  }

  @Test
  public void sequential() throws Throwable {

    ExecutorService executorService = spy(newCachedThreadPool());
    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenReturn(publisher -> from(publisher).publishOn(fromExecutorService(executorService)));

    from(strategy.forkJoin(testEvent(), fromIterable(getRoutingPairs(10)), processingStrategy, 1, true)).block();

    verify(executorService, never()).submit(any(Runnable.class));
  }

  private List<RoutingPair> getRoutingPairs(int number) {
    return range(0, number).mapToObj(i -> {
      try {
        return of(testEvent(), event -> event);
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    }).collect(toList());
  }

  private Processor createFailingRoutingPair(RuntimeException exception) {
    return event -> {
      throw exception;
    };
  }

  private Processor createFailingRoutingPair(RuntimeException exception, int sleep) {
    return event -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      throw exception;
    };
  }

  protected Processor createEchoProcessorSpy() {
    // Mockito does not support lambda
    return spy(new Processor() {

      @Override
      public Event process(Event event) throws MuleException {
        System.out.println(Thread.currentThread());
        return event;
      }
    });
  }

  private RoutingPair createSleepingRoutingPair(Event result, long sleep) throws MuleException {
    return of(testEvent(), event -> {
      try {
        sleep(sleep);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return result;
    });
  }

}
