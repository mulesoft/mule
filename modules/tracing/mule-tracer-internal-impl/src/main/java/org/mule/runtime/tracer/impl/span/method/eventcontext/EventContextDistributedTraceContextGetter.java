/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextGetter;

import javax.annotation.Nullable;

/**
 * A {@link org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter} that gets the
 * {@link DistributedTraceContextGetter} from the {@link EventContext}.
 *
 * @since 4.5.0
 */
public class EventContextDistributedTraceContextGetter implements DistributedTraceContextGetter<EventContext> {

  private static DistributedTraceContextGetter<EventContext> INSTANCE = new EventContextDistributedTraceContextGetter();

  public static DistributedTraceContextGetter<EventContext> getDistributedTraceContextGetter() {
    return INSTANCE;
  }

  private EventContextDistributedTraceContextGetter() {}

  @Nullable
  @Override
  public SpanContext getDistributedTraceContext(EventContext carrier) {
    if (carrier instanceof SpanContextAware) {
      return ((SpanContextAware) carrier).getSpanContext();
    }

    return null;
  }
}
