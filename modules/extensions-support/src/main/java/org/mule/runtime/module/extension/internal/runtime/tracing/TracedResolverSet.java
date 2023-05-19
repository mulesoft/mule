/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.tracing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.Map;

/**
 * An extended {@link ResolverSet} that will trace the resolution of its composed {@link ValueResolver}s.
 * 
 * @since 4.5
 */
public class TracedResolverSet extends ResolverSet {

  private final EventTracer<CoreEvent> coreEventEventTracer;
  private final InitialSpanInfo valueResolutionInitialSpanInfo;

  public TracedResolverSet(MuleContext muleContext, EventTracer<CoreEvent> coreEventEventTracer,
                           InitialSpanInfo valueResolutionInitialSpanInfo) {
    super(muleContext);
    this.coreEventEventTracer = coreEventEventTracer;
    this.valueResolutionInitialSpanInfo = valueResolutionInitialSpanInfo;
  }

  @Override
  protected Object resolve(Map.Entry<String, ValueResolver<?>> entry, ValueResolvingContext valueResolvingContext)
      throws MuleException {
    coreEventEventTracer.startComponentSpan(valueResolvingContext.getEvent(), valueResolutionInitialSpanInfo);
    try {
      coreEventEventTracer.addCurrentSpanAttribute(valueResolvingContext.getEvent(), "value-name", entry.getKey());
      return super.resolve(entry, valueResolvingContext);
    } finally {
      coreEventEventTracer.endCurrentSpan(valueResolvingContext.getEvent());
    }
  }
}
