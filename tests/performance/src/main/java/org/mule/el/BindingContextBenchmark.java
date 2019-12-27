/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.runtime.api.el.BindingContextUtils.ATTRIBUTES;
import static org.mule.runtime.api.el.BindingContextUtils.CORRELATION_ID;
import static org.mule.runtime.api.el.BindingContextUtils.ERROR;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.BindingContextUtils;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.TearDown;

@OutputTimeUnit(NANOSECONDS)
public class BindingContextBenchmark extends AbstractBenchmark {

  private static final CoreEvent event = CoreEvent.builder(create("", "", CONNECTOR_LOCATION, null, empty()))
      .message(of(PAYLOAD)).addVariable("foo", "bar").build();

  private static final BindingContext globalCtx = BindingContext.builder()
      .addBinding("onParent", new TypedValue<>("hello!", DataType.STRING))
      .build();

  private static final BindingContext childCtx = BindingContext.builder(globalCtx)
      .addBinding("onChild", new TypedValue<>("hello!", DataType.STRING))
      .build();

  private static final BindingContext bctx = event.asBindingContext();

  @TearDown
  public void teardown() throws MuleException {}

  @Benchmark
  public Object fromEvent() {
    return addEventBindings(event, NULL_BINDING_CONTEXT);
  }

  @Benchmark
  public Object withParent() {
    return BindingContext.builder(globalCtx)
        .addBinding("onChild", new TypedValue<>("hello!", DataType.STRING))
        .build();
  }

  @Benchmark
  public Object addAllThreeTimes() {
    return BindingContext.builder(globalCtx)
        .addAll(childCtx)
        .addAll(event.asBindingContext())
        .build();
  }

  @Benchmark
  public Object payloadBinding() {
    return bctx.lookup(BindingContextUtils.PAYLOAD);
  }

  @Benchmark
  public Object varsBinding() {
    return bctx.lookup(VARS);
  }

  @Benchmark
  public Object threeBindings() {
    return asList(bctx.lookup(BindingContextUtils.PAYLOAD), bctx.lookup(ATTRIBUTES), bctx.lookup(VARS));
  }

  @Benchmark
  public Object fiveBindings() {
    return asList(bctx.lookup(BindingContextUtils.PAYLOAD), bctx.lookup(ATTRIBUTES), bctx.lookup(VARS), bctx.lookup(ERROR),
                  bctx.lookup(CORRELATION_ID));
  }

  @Benchmark
  public Object parentLookup() {
    return childCtx.lookup("onParent");
  }
}
