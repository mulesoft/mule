/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.lang.Class.forName;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.mule.BenchmarkUtils.createFlow;
import static org.mule.BenchmarkUtils.createMuleContextWithServices;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.TriggerableMessageSource;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@State(Benchmark)
@Fork(1)
@Threads(1)
@BenchmarkMode(AverageTime)
@OutputTimeUnit(MICROSECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class FlowBenchmark {

  public static final String TEST_PAYLOAD = "test";
  public static final String TEST_CONNECTOR = "test";

  private MuleContext muleContext;
  private Flow flow;
  private TriggerableMessageSource source;

  @Param({"org.mule.runtime.core.processor.strategy.LegacySynchronousProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.ReactorProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.MultiReactorProcessingStrategyFactory",
      "org.mule.runtime.core.processor.strategy.MonoProcesingStrategyFactory"})
  public String processingStrategyFactory;

  @Setup
  public void setup() throws Exception {
    muleContext = createMuleContextWithServices();
    muleContext.start();

    source = new TriggerableMessageSource();
    flow = createFlow(muleContext);
    flow.setMessageProcessors(singletonList(event -> event));
    flow.setMessageSource(source);
    flow.setProcessingStrategyFactory((ProcessingStrategyFactory) forName(processingStrategyFactory).newInstance());
    muleContext.getRegistry().registerFlowConstruct(flow);
  }

  @TearDown
  public void teardown() {
    muleContext.dispose();
  }

  @Benchmark
  public Event processSourceOneWay() throws MuleException {
    return source.trigger(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of(TEST_PAYLOAD)).exchangePattern(ONE_WAY).build());
  }

  @Benchmark
  public Event processSourceRequestResponse() throws MuleException {
    return source.trigger(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of(TEST_PAYLOAD)).exchangePattern(REQUEST_RESPONSE).build());
  }

}
