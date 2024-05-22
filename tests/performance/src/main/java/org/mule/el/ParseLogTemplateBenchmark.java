/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import static org.openjdk.jmh.annotations.Threads.MAX;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@OutputTimeUnit(NANOSECONDS)
@Threads(MAX)
public class ParseLogTemplateBenchmark extends AbstractBenchmark {

  private ExtendedExpressionManager expressionManager;
  private MuleContext muleContext;
  private CoreEvent event;

  @Setup
  public void setup() throws MuleException {
    muleContext = createMuleContextWithServices();
    expressionManager = muleContext.getExpressionManager();
    EventContext context = create(createFlow(muleContext), CONNECTOR_LOCATION);
    event = CoreEvent.builder(context).message(of(PAYLOAD)).build();
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(muleContext.getSchedulerService());
    muleContext.dispose();
  }

  @Benchmark
  public Object parseLogTemplatePayload() {
    return expressionManager.parseLogTemplate("#[payload]", event, CONNECTOR_LOCATION, NULL_BINDING_CONTEXT);
  }

  @Benchmark
  public Object parseLogTemplatePayloadNested() {
    return expressionManager.parseLogTemplate("#['payload is: #[payload]']", event, CONNECTOR_LOCATION, NULL_BINDING_CONTEXT);
  }

}
