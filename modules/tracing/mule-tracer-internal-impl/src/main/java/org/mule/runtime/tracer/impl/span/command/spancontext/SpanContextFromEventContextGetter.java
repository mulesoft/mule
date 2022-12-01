/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command.spancontext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.context.EventSpanContext;

import javax.annotation.Nullable;

import static org.mule.runtime.tracer.api.context.SpanContext.emptyDistributedTraceContext;
import static org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter.emptyTraceContextMapGetter;

/**
 * A {@link SpanContextContextGetter} that gets the {@link SpanContext} from the {@link EventContext}.
 *
 * @since 4.5.0
 */
public class SpanContextFromEventContextGetter implements SpanContextContextGetter<EventContext> {

  private static final SpanContextContextGetter<EventContext> INSTANCE = new SpanContextFromEventContextGetter();

  public static SpanContextContextGetter<EventContext> getSpanContextFromEventContextGetter() {
    return INSTANCE;
  }

  private SpanContextFromEventContextGetter() {}

  @Nullable
  @Override
  public SpanContext get(EventContext carrier) {
    if (carrier instanceof SpanContextAware) {
      SpanContextAware spanContextAwareCarrier = (SpanContextAware) carrier;
      SpanContext spanContext = spanContextAwareCarrier.getSpanContext();
      if (spanContext == null) {
        spanContext = EventSpanContext
            .builder()
            .withGetter(emptyTraceContextMapGetter())
            .build();
        spanContextAwareCarrier.setSpanContext(spanContext);
      }
      return spanContext;
    }

    return emptyDistributedTraceContext();
  }
}
