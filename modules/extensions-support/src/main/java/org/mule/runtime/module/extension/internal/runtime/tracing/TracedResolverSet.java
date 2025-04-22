/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.tracing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.tracer.api.component.ComponentTracer;

import java.util.Map;

/**
 * An extended {@link ResolverSet} that will trace the resolution of its composed {@link ValueResolver}s.
 * 
 * @since 4.5
 */
public class TracedResolverSet extends ResolverSet {

  private final ComponentTracer<CoreEvent> valueResolutionTracer;

  public TracedResolverSet(Injector injector,
                           ComponentTracer<CoreEvent> valueResolutionTracer) {
    super(injector);
    this.valueResolutionTracer = valueResolutionTracer;
  }

  @Override
  protected Object resolve(Map.Entry<String, ValueResolver<?>> entry, ValueResolvingContext valueResolvingContext)
      throws MuleException {
    valueResolutionTracer.startSpan(valueResolvingContext.getEvent());
    try {
      valueResolutionTracer.addCurrentSpanAttribute(valueResolvingContext.getEvent(), "value-name", entry.getKey());
      return super.resolve(entry, valueResolvingContext);
    } finally {
      valueResolutionTracer.endCurrentSpan(valueResolvingContext.getEvent());
    }
  }
}
