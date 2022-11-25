/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.distributedtracecontext;

import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.impl.span.method.EndEventSpanMethod;

import static org.mule.runtime.tracer.api.span.validation.Assertion.SUCCESSFUL_ASSERTION;

public class DistributedTraceContextEndSpanMethod implements EndEventSpanMethod<SpanContext> {

  private static final EndEventSpanMethod<SpanContext> INSTANCE = new DistributedTraceContextEndSpanMethod();

  public static EndEventSpanMethod<SpanContext> getDistributedTraceContextEndSpanMethod() {
    return INSTANCE;
  }

  private DistributedTraceContextEndSpanMethod() {}

  @Override
  public void end(SpanContext context) {
    end(context, SUCCESSFUL_ASSERTION);
  }

  @Override
  public void end(SpanContext context, Assertion condition) {
    context.endSpan(condition);
  }
}
