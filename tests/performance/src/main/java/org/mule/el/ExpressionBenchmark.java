/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

@OutputTimeUnit(NANOSECONDS)
public class ExpressionBenchmark extends AbstractBenchmark {


  private MuleContext muleContext;
  private CoreEvent event;
  private EventContext context;

  @Setup
  public void setup() throws MuleException {
    muleContext = createMuleContextWithServices();
    context = create(createFlow(muleContext), CONNECTOR_LOCATION);
    event = CoreEvent.builder(context).message(of(PAYLOAD)).addVariable("foo", "bar").build();
  }

  @TearDown
  public void teardown() throws MuleException {
    stopIfNeeded(lookupObject(muleContext, SchedulerService.class));
    muleContext.dispose();
  }

  @Benchmark
  public Object melPayload() {
    return muleContext.getExpressionManager().evaluate("mel:payload", event).getValue();
  }

  @Benchmark
  public Object dwPayload() {
    return muleContext.getExpressionManager().evaluate("payload", event).getValue();
  }

  @Benchmark
  public Object melFlowVars() {
    return muleContext.getExpressionManager().evaluate("mel:flowVars['foo']=='bar'", event).getValue();
  }

  @Benchmark
  public Object dwFlowVars() {
    return muleContext.getExpressionManager().evaluate("vars.foo == 'bar'", event).getValue();
  }

  @Benchmark
  public Object melGetLocale() {
    return muleContext.getExpressionManager().evaluate("mel:java.util.Locale.getDefault().getLanguage()", event).getValue();
  }

  @Benchmark
  public Object dwGetLocale() {
    return muleContext.getExpressionManager().evaluate("java!java::util::Locale::getDefault().language", event).getValue();
  }

}
