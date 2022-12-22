/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.optel.resources;

import io.opentelemetry.sdk.trace.IdGenerator;

public class ThreadIdGenerator implements IdGenerator {

  public static ThreadLocal<String> spanIdHolder = new ThreadLocal<>();

  @Override
  public String generateSpanId() {
    if (spanIdHolder.get() != null) {
      return spanIdHolder.get();
    }
    throw new IllegalStateException("No thread span id set");
  }

  @Override
  public String generateTraceId() {
    return IdGenerator.random().generateTraceId();
  }

  public static void setSpanIdHolder(String threadSpanId) {
    spanIdHolder.set(threadSpanId);
  }
}
