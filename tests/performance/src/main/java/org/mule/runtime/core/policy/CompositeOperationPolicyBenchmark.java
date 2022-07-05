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
import static org.mule.runtime.core.internal.execution.SourcePolicyTestUtils.block;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.internal.policy.CompositeOperationPolicy;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.reactivestreams.Publisher;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(MICROSECONDS)
public class CompositeOperationPolicyBenchmark extends AbstractBenchmark {

  private OperationPolicy handler;

  private Scheduler fluxCompleteScheduler;

  @Setup(Level.Trial)
  public void setUp() throws MuleException {
    MuleContext muleContext = createMuleContextWithServices();
    this.fluxCompleteScheduler = muleContext.getSchedulerService().ioScheduler();

    handler = new CompositeOperationPolicy(new AbstractComponent() {}, asList(new Policy(new PolicyChain() {

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        return publisher;
      }
    }, "")), empty(), (policy, nextProcessor) -> nextProcessor,
                                           muleContext.getConfiguration().getShutdownTimeout(),
                                           fluxCompleteScheduler, feature -> false);
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    this.fluxCompleteScheduler.stop();
  }

  @Benchmark
  @Threads(Threads.MAX)
  public CoreEvent source() throws Throwable {
    CoreEvent event;
    Message.Builder messageBuilder = Message.builder().value(PAYLOAD);
    CoreEvent.Builder eventBuilder =
        CoreEvent.builder(create("", "", CONNECTOR_LOCATION, null, empty())).message(messageBuilder.build());
    event = eventBuilder.build();

    Object value = block(outerCallback -> {
      ExecutorCallback executorCallback = new ExecutorCallback() {

        @Override
        public void complete(Object value) {
          outerCallback.complete(value);
        }

        @Override
        public void error(Throwable e) {
          outerCallback.error(e);
        }
      };
      handler.process(event,
                      (params, e, callback) -> callback.complete(e),
                      () -> emptyMap(),
                      CONNECTOR_LOCATION,
                      executorCallback);
    });

    return (CoreEvent) value;
  }
}
