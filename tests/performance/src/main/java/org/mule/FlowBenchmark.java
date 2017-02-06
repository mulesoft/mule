/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.lang.Class.forName;
import static java.util.Collections.singletonList;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.tck.TriggerableMessageSource;

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import reactor.core.publisher.Mono;

@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class FlowBenchmark extends AbstractBenchmark {

  public static final String TEST_PAYLOAD = "test";
  public static final String TEST_CONNECTOR = "test";

  private MuleContext muleContext;
  private Flow flow;
  private TriggerableMessageSource source;

  @Param({
      "org.mule.runtime.core.processor.strategy.LegacySynchronousProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.SynchronousStreamProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.ReactorProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.MultiReactorProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.ProactorProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.WorkQueueProcessingStrategyFactory"
  })
  public String processingStrategyFactory;

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();

    source = new TriggerableMessageSource();
    flow = builder(FLOW_NAME, muleContext).messageProcessors(singletonList(event -> event)).messageSource(source)
        .processingStrategyFactory((ProcessingStrategyFactory) forName(processingStrategyFactory).newInstance()).build();
    muleContext.getRegistry().registerFlowConstruct(flow);
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(muleContext.getRegistry().lookupObject(SchedulerService.class));
    muleContext.dispose();
  }

  @Benchmark
  public Event processSource() throws MuleException {
    return source.trigger(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of(TEST_PAYLOAD)).build());
  }

  @Benchmark
  public CountDownLatch processSourceStream() throws MuleException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(1000);
    for (int i = 0; i < 1000; i++) {
      Mono.just(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
          .message(InternalMessage.of(TEST_PAYLOAD)).build()).transform(source.getListener()).doOnNext(event -> latch.countDown())
          .subscribe();
    }
    latch.await();
    return latch;
  }

  @Benchmark
  public Event processFlow() throws MuleException {
    return flow.process(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of(TEST_PAYLOAD)).build());
  }

  @Benchmark
  public CountDownLatch processFlowStream() throws MuleException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(1000);
    for (int i = 0; i < 1000; i++) {
      Mono.just(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
          .message(InternalMessage.of(TEST_PAYLOAD)).build()).transform(flow).doOnNext(event -> latch.countDown()).subscribe();
    }
    latch.await();
    return latch;
  }

}
