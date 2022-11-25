/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.distributedtracecontext;

import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.method.AddSpanAttributeMethod;

import java.util.Map;

public class DistributedTraceContextAddSpanAttributeMethod implements AddSpanAttributeMethod<SpanContext> {

  public static AddSpanAttributeMethod<SpanContext> getDistributedTraceContextAddSpanAttributeMethod() {
    return new DistributedTraceContextAddSpanAttributeMethod();
  }

  private DistributedTraceContextAddSpanAttributeMethod() {}

  @Override
  public void addAttribute(SpanContext context, String key, String value) {
    context.getSpan().ifPresent(span -> span.addAttribute(key, value));
  }

  @Override
  public void addAttributes(SpanContext context, Map<String, String> attributes) {
    context.getSpan().ifPresent(span -> attributes.forEach(span::addAttribute));
  }
}
