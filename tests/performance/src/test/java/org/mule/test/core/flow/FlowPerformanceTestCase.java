/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.flow;

import static java.time.temporal.ChronoUnit.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.config.builders.BasicRuntimeServicesConfigurationBuilder;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.strategy.MultiReactorProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.ProactorProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.ReactorProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory;
import org.mule.runtime.core.api.rx.Exceptions.EventDroppedException;
import org.mule.service.scheduler.internal.DefaultSchedulerService;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the performance of different approach of invoking a flow (blocking, mono, flux) along with the different processing
 * strategies.
 */
@Features("Performance Tests")
@Stories("Flow Performance Tests")
@RunWith(Parameterized.class)
public class FlowPerformanceTestCase extends AbstractMuleContextTestCase {

  private static Processor liteProcessor = event -> event;

  private Flow flow;
  private TriggerableMessageSource source;
  private BlockingSink<Event> fluxSink;
  private ProcessingStrategyFactory processingStrategyFactory;
  private List<Processor> processors;
  private FluxProcessor<Event, Event> fluxProcessor;

  private static int ITERATIONS = 10000;

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  public FlowPerformanceTestCase(ProcessingStrategyFactory processingStrategyFactory, List<Processor> processors) {
    this.processingStrategyFactory = processingStrategyFactory;
    this.processors = processors;
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    return asList(new Object[][] {
        {new SynchronousProcessingStrategyFactory(), singletonList(liteProcessor)},
        {new ReactorProcessingStrategyFactory(), singletonList(liteProcessor)},
        {new MultiReactorProcessingStrategyFactory(), singletonList(liteProcessor)},
        {new ProactorProcessingStrategyFactory(), singletonList(liteProcessor)},
        {new RingBufferProcessingStrategyFactory(), singletonList(liteProcessor)},
        {new ParellelProcessingStrategyFactory(), singletonList(liteProcessor)}
    });
  }

  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new BasicRuntimeServicesConfigurationBuilder());
  }

  @Before
  public void setup() throws MuleException {
    muleContext.start();

    source = new TriggerableMessageSource();
    flow = new Flow("flow", muleContext);
    flow.setMessageProcessors(processors);
    flow.setMessageSource(source);
    flow.setProcessingStrategyFactory(processingStrategyFactory);
    muleContext.getRegistry().registerFlowConstruct(flow);

    fluxProcessor = EmitterProcessor.create(1);
    fluxProcessor.flatMap(event -> source.triggerAsync(event))
        .doOnError(EventDroppedException.class, mde -> mde.getEvent().getContext().success())
        .doOnNext(response -> response.getContext().success(response))
        .doOnError(MessagingException.class, me -> me.getEvent().getContext().error(me))
        .subscribe();
    fluxSink = fluxProcessor.connectSink();
  }

  @After
  public void cleanup() throws MuleException {
    fluxSink.complete();
    ((DefaultSchedulerService) muleContext.getSchedulerService()).stop();
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  @Description("Invoke Flow `ITERATIONS` times using blocking 3.x.")
  public void blocking() throws Exception {
    for (int i = 0; i < ITERATIONS; i++) {
      source.trigger(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
          .message(InternalMessage.of(TEST_PAYLOAD)).build());
    }
  }

  @Ignore
  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  @Description("Invoke Flow `ITERATIONS` times using a Mono per request and waiting for Mono completion.")
  public void mono() throws Exception {
    for (int i = 0; i < ITERATIONS; i++) {
      Mono.just(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).message(InternalMessage.of(TEST_PAYLOAD))
          .build()).then(event -> from(source.triggerAsync(event))).block(Duration.of(LOCK_TIMEOUT, MILLIS));
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  @Description("Invoke Flow by pushing `ITERATIONS` events via Flux and waiting for completion of each one.")
  public void fluxPush() throws Exception {
    CountDownLatch latch = new CountDownLatch(ITERATIONS);
    for (int i = 0; i < ITERATIONS; i++) {
      Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
          .message(InternalMessage.of(TEST_PAYLOAD)).build();
      fluxSink.accept(event);
      from(event.getContext()).doOnNext(e -> latch.countDown()).subscribe();
    }
    latch.await(LOCK_TIMEOUT, SECONDS);
  }

  @Ignore
  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  @Description("Invoke Flow by pulling `ITERATIONS` events via Flux and waiting for completion of each one.")
  public void fluxPull() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    CountDownLatch latch = new CountDownLatch(ITERATIONS);

    Flux.<Event>generate(eventSynchronousSink -> {
      if (requests.getAndIncrement() < ITERATIONS) {
        // Create Event
        Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
            .message(InternalMessage.of(TEST_PAYLOAD)).build();
        // Subscriber to EventContext to countdown latch when response is notified.
        from(event.getContext())
            .doOnNext(e -> latch.countDown())
            .doOnError(MessagingException.class, e -> e.getEvent().getContext().error(e))
            .subscribe();
        // Fire event
        eventSynchronousSink.next(event);
      } else {
        // Complete Flux once `ITERATIONS` sent.
        eventSynchronousSink.complete();
      }
    }).flatMap(event -> source.triggerAsync(event))
        .doOnError(EventDroppedException.class, mde -> mde.getEvent().getContext().success())
        .doOnNext(response -> response.getContext().success(response))
        .doOnError(MessagingException.class, me -> me.getEvent().getContext().error(me))
        .subscribe();
    latch.await(LOCK_TIMEOUT, SECONDS);
  }

}
