/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.distributedtracecontext;

import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.method.SetCurrentSpanNameMethod;

public class DistributedTraceContextSetCurrentSpanNameMethod implements SetCurrentSpanNameMethod<SpanContext> {

  public static SetCurrentSpanNameMethod<SpanContext> getDistributedTraceContextSetCurrentSpanNameMethod() {
    return new DistributedTraceContextSetCurrentSpanNameMethod();
  }

  private DistributedTraceContextSetCurrentSpanNameMethod() {}

  @Override
  public void setCurrentSpanName(SpanContext context, String name) {
    context.getSpan().ifPresent(span -> span.updateName(name));
  }
}
