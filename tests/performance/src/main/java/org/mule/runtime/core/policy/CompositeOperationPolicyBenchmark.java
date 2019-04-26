/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import org.mule.AbstractBenchmark;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.internal.policy.CompositeOperationPolicy;
import org.mule.runtime.core.internal.policy.OperationPolicy;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Threads;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(MICROSECONDS)
public class CompositeOperationPolicyBenchmark extends AbstractBenchmark {

  private OperationPolicy handler;

  @Setup(Level.Trial)
  public void setUp() {
    handler = new CompositeOperationPolicy(asList(new Policy(new PolicyChain() {

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        return publisher;
      }
    }, "")), empty(), (policy, nextProcessor) -> nextProcessor);
  }

  @Benchmark
  @Threads(Threads.MAX)
  public CoreEvent source() {
    CoreEvent event;
    Message.Builder messageBuilder = Message.builder().value(PAYLOAD);
    CoreEvent.Builder eventBuilder =
        CoreEvent.builder(create("", "", CONNECTOR_LOCATION, NullExceptionHandler.getInstance())).message(messageBuilder.build());
    event = eventBuilder.build();

    return Mono.<CoreEvent>create(outerSink -> handler.process(event,
                                                               (params, e, sink) -> sink.success(e),
                                                               () -> emptyMap(),
                                                               CONNECTOR_LOCATION,
                                                               outerSink))
        .block();
  }
}
