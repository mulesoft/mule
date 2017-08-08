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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.TIMEOUT;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair.of;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.internal.routing.CompositeRoutingException;
import org.mule.runtime.core.internal.routing.RoutingResult;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public abstract class AbstractForkJoinStrategyTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected ForkJoinStrategy strategy;
  protected ProcessingStrategy processingStrategy;
  private Scheduler ioScheduler;
  private Flow flow;
  protected ErrorType timeoutErrorType;

  @Before
  public void setup() {
    processingStrategy = mock(ProcessingStrategy.class);

    Function<ReactiveProcessor, ReactiveProcessor> scheduleFunction =
        processor -> publisher -> from(publisher).publishOn(fromExecutorService(ioScheduler)).transform(processor);
    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenAnswer(invocation -> scheduleFunction.apply(invocation.getArgumentAt(0, ReactiveProcessor.class)));
    ioScheduler = muleContext.getSchedulerService().ioScheduler();
    flow = mock(Flow.class);
    timeoutErrorType = muleContext.getErrorTypeRepository().getErrorType(TIMEOUT).get();
    strategy = createStrategy(processingStrategy, Integer.MAX_VALUE, true, MAX_VALUE);
  }

  @After
  public void tearown() {
    ioScheduler.stop();
  }

  protected abstract ForkJoinStrategy createStrategy(ProcessingStrategy processingStrategy, int concurrency, boolean delayErrors,
                                                     long timeout);

  @Test
  public void timeout() throws Throwable {
    Event original = testEvent();

    RoutingPair timeoutPair = createSleepingRoutingPair(Message.of("1"), 100);

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(createStrategy(processingStrategy, 2, true, 50), original, asList(timeoutPair),
                           throwable -> {
                             CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 1);
                             RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 0, 1);
                             assertThat(routingResult.getFailures().get(Integer.toString(0)).getCause(),
                                        instanceOf(TimeoutException.class));
                           });
  }

  @Test
  public void timeoutDelayed() throws Throwable {
    Event original = testEvent();
    Message route1Result = Message.of("1");
    Message route2Result = Message.of("2");

    RoutingPair timeoutPair = createSleepingRoutingPair(route1Result, 100);
    RoutingPair otherPair = of(testEvent(), createChain(event -> Event.builder(event).message(route2Result).build()));

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(createStrategy(processingStrategy, 2, true, 50), original, asList(timeoutPair, otherPair),
                           throwable -> {
                             CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 1);
                             RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 1, 1);
                             assertThat(routingResult.getFailures().get(Integer.toString(0)).getCause(),
                                        instanceOf(TimeoutException.class));
                             assertThat(routingResult.getResults().get(Integer.toString(1)), is(route2Result));
                           });
  }

  @Test
  public void timeoutEager() throws Throwable {
    Event original = testEvent();
    Message route1Result = Message.of("1");
    Message route2Result = Message.of("2");

    RoutingPair timeoutPair = createSleepingRoutingPair(route1Result, 100);
    RoutingPair otherPair = of(testEvent(), createChain(event -> Event.builder(event).message(route2Result).build()));

    expectedException.expect(instanceOf(DefaultMuleException.class));
    expectedException.expectCause(instanceOf(TimeoutException.class));

    invokeStrategyBlocking(createStrategy(processingStrategy, 2, false, 50), original, asList(timeoutPair, otherPair));
  }


  @Test
  public void error() throws Throwable {
    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(testEvent(), createFailingRoutingPair(exception));

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(strategy, testEvent(), asList(failingPair), throwable -> {
      CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 1);
      RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 0, 1);
      assertThat(routingResult.getFailures().get(Integer.toString(0)).getCause(), is(exception));
    });
  }

  @Test
  public void errorDelayed() throws Throwable {
    Processor processorSpy = createEchoProcessorSpy();

    RuntimeException exception1 = new IllegalStateException();
    RoutingPair failingPair1 = of(testEvent(), createFailingRoutingPair(exception1));
    RuntimeException exception2 = new UnsupportedOperationException();
    RoutingPair failingPair2 = of(testEvent(), createFailingRoutingPair(exception2));
    RuntimeException exception3 = new IndexOutOfBoundsException();
    RoutingPair failingPair3 = of(testEvent(), createFailingRoutingPair(exception3));
    RoutingPair okPair = of(testEvent(), createChain(processorSpy));

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(instanceOf(CompositeRoutingException.class));

    invokeStrategyBlocking(strategy, testEvent(), asList(failingPair1, failingPair2, failingPair3, okPair), throwable -> {
      verify(processorSpy, times(1)).process(any(Event.class));
      CompositeRoutingException compositeRoutingException = assertCompositeRoutingException(throwable, 3);
      RoutingResult routingResult = assertRoutingResult(compositeRoutingException, 1, 3);
      assertThat(routingResult.getFailures().get(Integer.toString(0)).getCause(), is(exception1));
      assertThat(routingResult.getFailures().get(Integer.toString(1)).getCause(), is(exception2));
      assertThat(routingResult.getFailures().get(Integer.toString(2)).getCause(), is(exception3));
      assertThat(routingResult.getFailures().get(Integer.toString(3)), is(nullValue()));
    });

  }

  @Test
  public void errorEager() throws Throwable {
    Processor processorSpy = createEchoProcessorSpy();

    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(testEvent(), createFailingRoutingPair(exception));
    RoutingPair okPair = of(testEvent(), createChain(processorSpy));

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(is(exception));

    invokeStrategyBlocking(createStrategy(processingStrategy, 2, false, MAX_VALUE), testEvent(), asList(failingPair, okPair),
                           throwable -> verify(processorSpy, never()).process(any(Event.class)));
  }

  @Test
  public void errorEagerConcurrent() throws Throwable {
    Processor processorSpy = createEchoProcessorSpy();
    Processor processorSpy2 = createEchoProcessorSpy();
    Processor processorSpy3 = createEchoProcessorSpy();

    Event orignial = testEvent();
    RuntimeException exception = new IllegalStateException();
    RoutingPair failingPair = of(orignial, createFailingRoutingPair(exception));
    RoutingPair okPair = of(orignial, createChain(processorSpy));
    RoutingPair okPair2 = of(orignial, createChain(processorSpy2));
    RoutingPair okPair3 = of(orignial, createChain(processorSpy3));

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectCause(is(exception));

    invokeStrategyBlocking(createStrategy(processingStrategy, 4, false, MAX_VALUE), testEvent(),
                           asList(failingPair, okPair, okPair2, okPair3), throwable -> {
                             verify(processorSpy, atMost(1)).process(any(Event.class));
                             verify(processorSpy2, atMost(1)).process(any(Event.class));
                             verify(processorSpy3, atMost(1)).process(any(Event.class));
                           });
  }

  @Test
  public void flowVarsMerged() throws Throwable {
    String beforeVarName = "before";
    String fooVarName = "foo";
    String foo2VarName = "foo2";

    Event original = builder(newEvent()).addVariable(beforeVarName, "value").build();

    RoutingPair pair1 = of(testEvent(), createChain(event -> builder(event).addVariable(fooVarName, "bar").build()));
    RoutingPair pair2 = of(testEvent(), createChain(event -> builder(event).addVariable(foo2VarName, "bar2").build()));

    Event result = invokeStrategyBlocking(strategy, original, asList(pair1, pair2));

    assertThat(result.getVariableNames(), hasSize(3));
    assertThat(result.getVariableNames(), hasItems(beforeVarName, fooVarName, foo2VarName));
  }

  @Test
  public void concurrent() throws Throwable {
    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenReturn(publisher -> from(publisher).publishOn(fromExecutorService(ioScheduler)));

    int pairs = 10;

    invokeStrategyBlocking(createStrategy(processingStrategy, 2, true, MAX_VALUE), testEvent(), getRoutingPairs(pairs));

    verify(ioScheduler, times(pairs)).submit(any(Runnable.class));
  }

  @Test
  public void sequential() throws Throwable {
    ExecutorService executorService = spy(newCachedThreadPool());
    when(processingStrategy.onPipeline(any(ReactiveProcessor.class)))
        .thenReturn(publisher -> from(publisher).publishOn(fromExecutorService(executorService)));

    invokeStrategyBlocking(createStrategy(processingStrategy, 1, true, MAX_VALUE), testEvent(), getRoutingPairs(10));

    verify(executorService, never()).submit(any(Runnable.class));
  }

  private CompositeRoutingException assertCompositeRoutingException(Throwable throwable, int errors) {
    CompositeRoutingException compositeRoutingException = (CompositeRoutingException) throwable.getCause();
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

  protected Event invokeStrategyBlocking(ForkJoinStrategy strategy, Event original, List<RoutingPair> routingPairs)
      throws Throwable {
    return invokeStrategyBlocking(strategy, original, routingPairs, throwable -> {
    });
  }

  protected Event invokeStrategyBlocking(ForkJoinStrategy strategy, Event original, List<RoutingPair> routingPairs,
                                         CheckedConsumer<Throwable> verifyOnError)
      throws Throwable {
    try {
      return from(strategy.forkJoin(original, fromIterable(routingPairs))).block();
    } catch (Throwable throwable) {
      throwable = rxExceptionToMuleException(throwable);
      verifyOnError.accept(throwable);
      throw throwable;
    } finally {

    }
  }

  private List<RoutingPair> getRoutingPairs(int number) {
    return range(0, number).mapToObj(i -> {
      try {
        return of(testEvent(), createChain(event -> event));
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    }).collect(toList());
  }

  private MessageProcessorChain createFailingRoutingPair(RuntimeException exception) throws MuleException {
    return createChain(event -> {
      throw exception;
    });
  }

  protected Processor createEchoProcessorSpy() throws MuleException {
    // Mockito does not support lambda
    return spy(new Processor() {

      @Override
      public Event process(Event event) throws MuleException {
        return event;
      }
    });
  }

  private RoutingPair createSleepingRoutingPair(Message result, long sleep) throws MuleException {
    return of(testEvent(), createChain(event -> {
      try {
        sleep(sleep);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return Event.builder(event).message(result).build();
    }));
  }

  protected MessageProcessorChain createChain(Processor processor) throws MuleException {
    MessageProcessorChain chain = newChain(processor);
    chain.setMuleContext(muleContext);
    chain.setFlowConstruct(flow);
    return chain;
  }

}
