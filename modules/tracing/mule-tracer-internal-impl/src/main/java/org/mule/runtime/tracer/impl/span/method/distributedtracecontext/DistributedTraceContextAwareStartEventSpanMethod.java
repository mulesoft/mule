/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.distributedtracecontext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.impl.span.method.StartEventSpanMethod;

import java.util.Optional;

import static java.util.Optional.empty;

public class DistributedTraceContextAwareStartEventSpanMethod implements StartEventSpanMethod<EventContext> {

  private final StartEventSpanMethod<SpanContext> delegate;

  public StartEventSpanMethod<EventContext> getDistributedTraceContextAwareStartEventSpanMethod(StartEventSpanMethod<SpanContext> eventSpanMethod) {
    return new DistributedTraceContextAwareStartEventSpanMethod(eventSpanMethod);
  }

  private DistributedTraceContextAwareStartEventSpanMethod(StartEventSpanMethod<SpanContext> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Optional<InternalSpan> start(EventContext context, CoreEvent coreEvent, StartSpanInfo spanCustomizationInfo) {
    if (context instanceof SpanContextAware) {
      return delegate.start((SpanContext) context, coreEvent, spanCustomizationInfo);
    }
    return empty();
  }

  @Override
  public Optional<InternalSpan> start(EventContext context, CoreEvent coreEvent, StartSpanInfo spanCustomizationInfo,
                                      Assertion assertion) {
    if (context instanceof SpanContextAware) {
      return delegate.start((SpanContext) context, coreEvent, spanCustomizationInfo, assertion);
    }
    return empty();
  }
}
