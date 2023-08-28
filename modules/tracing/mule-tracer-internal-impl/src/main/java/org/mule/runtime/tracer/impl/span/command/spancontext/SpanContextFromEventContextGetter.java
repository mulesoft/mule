/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command.spancontext;

import static org.mule.runtime.tracer.api.context.SpanContext.emptySpanContext;
import static org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter.emptyTraceContextMapGetter;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.impl.context.EventSpanContext;

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

    return emptySpanContext();
  }
}
