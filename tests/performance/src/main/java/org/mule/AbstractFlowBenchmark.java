/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.lang.Class.forName;
import static java.lang.Thread.sleep;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.openjdk.jmh.infra.Blackhole.consumeCPU;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.processor.strategy.AbstractProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.ReactorStreamProcessingStrategyFactory;
import org.mule.tck.TriggerableMessageSource;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import reactor.core.publisher.Mono;

@State(Scope.Benchmark)
public abstract class AbstractFlowBenchmark extends AbstractBenchmark {

  static final Processor nullProcessor = event -> event;

  static final Processor cpuLightProcessor = event -> {
    // Roughly 50uS on modern CPU.
    consumeCPU(25000);
    return event;
  };

  static final Processor cpuIntensiveProcessor = new Processor() {

    @Override
    public Event process(Event event) throws MuleException {
      // Roughly 5mS on modern CPU.
      consumeCPU(2500000);
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return ProcessingType.CPU_INTENSIVE;
    }
  };

  static final Processor blockingProcessor = new Processor() {

    @Override
    public Event process(Event event) throws MuleException {
      try {
        sleep(20);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return event;
    }

    @Override
    public ProcessingType getProcessingType() {
      return ProcessingType.BLOCKING;
    }
  };

  protected MuleContext muleContext;
  protected Flow flow;
  protected TriggerableMessageSource source;

  @Param({
      "org.mule.runtime.core.processor.strategy.DirectProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.DirectStreamPerThreadProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.ReactorProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.ReactorStreamProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.DefaultStreamProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.WorkQueueProcessingStrategyFactory",
      // Skipping due MULE-12662.
      // "org.mule.runtime.core.processor.strategy.WorkQueueStreamProcessingStrategyFactory",
  })
  public String processingStrategyFactory;

  @Param({"1"})
  public int subscribers;

  @Param({"256"})
  public int bufferSize;

  @Param({"10000"})
  public int maxConcurrency;

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();

    ProcessingStrategyFactory factory = (ProcessingStrategyFactory) forName(processingStrategyFactory).newInstance();
    if (factory instanceof AbstractProcessingStrategyFactory) {
      ((AbstractProcessingStrategyFactory) factory).setMaxConcurrency(maxConcurrency);
    }
    if (factory instanceof ReactorStreamProcessingStrategyFactory) {
      ((ReactorStreamProcessingStrategyFactory) factory).setBufferSize(bufferSize);
      ((ReactorStreamProcessingStrategyFactory) factory).setSubscriberCount(subscribers);
    }


    source = new TriggerableMessageSource();
    flow = builder(FLOW_NAME, muleContext).messageProcessors(getMessageProcessors()).messageSource(source)
        .processingStrategyFactory(factory).build();
    muleContext.getRegistry().registerFlowConstruct(flow);
  }

  protected abstract List<Processor> getMessageProcessors();

  protected abstract int getStreamIterations();

  @TearDown
  public void teardown() throws MuleException {
    SchedulerService schedulerService = muleContext.getRegistry().lookupObject(SchedulerService.class);
    muleContext.dispose();
    stopIfNeeded(schedulerService);
  }

  @Benchmark
  public Event processSourceBlocking() throws MuleException {
    return source.trigger(Event.builder(DefaultEventContext.create(flow, CONNECTOR_LOCATION))
        .message(of(PAYLOAD)).build());
  }

  @Benchmark
  public CountDownLatch processSourceStream() throws MuleException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(getStreamIterations());
    for (int i = 0; i < getStreamIterations(); i++) {
      Mono.just(Event.builder(DefaultEventContext.create(flow, CONNECTOR_LOCATION))
          .message(of(PAYLOAD)).build()).transform(source.getListener()).doOnNext(event -> latch.countDown())
          .subscribe();
    }
    latch.await();
    return latch;
  }

}
