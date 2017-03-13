/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class SimpleExpressionBenchmark extends AbstractBenchmark {

  @Param({"mel:payload", "payload"})
  private String expression;

  @Param({"mel:flowVars['foo']", "foo"})
  private String expression2;

  private MuleContext muleContext;
  private Event event;

  @Setup
  public void setup() throws MuleException {
    muleContext = createMuleContextWithServices();
    event = Event.builder(DefaultEventContext.create(createFlow(muleContext), "")).addVariable("foo", "bar")
        .addVariable("foo", "bar").message(Message.builder().payload(PAYLOAD).build()).build();
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(muleContext.getRegistry().lookupObject(SchedulerService.class));
    muleContext.dispose();
  }

  @Benchmark
  public Object test() {
    String[] result = new String[20];
    result[0] = (String) muleContext.getExpressionManager().evaluate(expression, event).getValue();
    for (int i = 1; i < 20; i++) {
      result[i] = (String) muleContext.getExpressionManager().evaluate(expression2, event).getValue();
    }
    return result;
  }

}
