/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;

import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;

@OutputTimeUnit(MILLISECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class ProcessorChainBenchmark extends AbstractBenchmark {

  private static final int NUM_PROCESSORS = 20;
  private static final int STREAM_SIZE = 1000;
  private MessageProcessorChain chain;

  private CoreEvent event;

  @Setup
  public void setup() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    for (int i = 0; i < NUM_PROCESSORS; i++) {
      builder.chain(event -> event);
    }
    chain = builder.build();
    chain.setMuleContext(createMuleContextWithServices());
    event = builder(create("", "", CONNECTOR_LOCATION, null, empty()))
        .message(of(PAYLOAD)).build();
  }

  @Benchmark
  public CoreEvent blocking() throws MuleException {
    return chain.process(event);
  }

  @Benchmark
  public CountDownLatch stream() throws MuleException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(STREAM_SIZE);
    Reference<FluxSink<CoreEvent>> sinkReference = new Reference<>();
    FluxProcessor.create(sinkReference::set)
        .transform(chain)
        .doOnNext(event -> latch.countDown())
        .subscribe();
    for (int i = 0; i < STREAM_SIZE; i++) {
      sinkReference.get().next(event);
    }
    sinkReference.get().complete();
    latch.await();
    return latch;
  }

}
