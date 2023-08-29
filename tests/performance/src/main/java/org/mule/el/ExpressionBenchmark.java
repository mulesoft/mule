/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

@OutputTimeUnit(NANOSECONDS)
// TODO MULE-15707: MVEL hangs with this level of concurrency
// @Threads(MAX)
public class ExpressionBenchmark extends AbstractBenchmark {

  private ExtendedExpressionManager expressionManager;
  private MuleContext muleContext;
  private CoreEvent event;
  private EventContext context;

  @Setup
  public void setup() throws MuleException {
    muleContext = createMuleContextWithServices();
    expressionManager = muleContext.getExpressionManager();
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
    return expressionManager.evaluate("mel:payload", event).getValue();
  }

  @Benchmark
  public Object dwPayload() {
    return expressionManager.evaluate("payload", event.asBindingContext()).getValue();
  }

  @Benchmark
  public Object melFlowVars() {
    return expressionManager.evaluate("mel:flowVars['foo']=='bar'", event).getValue();
  }

  @Benchmark
  public Object dwFlowVars() {
    return expressionManager.evaluate("vars.foo == 'bar'", event.asBindingContext()).getValue();
  }

  @Benchmark
  public Object melGetLocale() {
    return expressionManager.evaluate("mel:java.util.Locale.getDefault().getLanguage()", event).getValue();
  }

  @Benchmark
  public Object dwGetLocale() {
    return expressionManager.evaluate("java!java::util::Locale::getDefault().language", event.asBindingContext()).getValue();
  }

}
