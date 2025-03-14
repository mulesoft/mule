/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;

import jakarta.inject.Inject;

/**
 * Factory that allows the creation of {@link ComponentTracer} instances.
 *
 * @since 4.5.0
 */
public class CoreEventComponentTracerFactory implements ComponentTracerFactory<CoreEvent> {

  @Inject
  private InitialSpanInfoProvider initialSpanInfoProvider;

  @Inject
  private EventTracer<CoreEvent> coreEventTracer;

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component) {
    InitialSpanInfo initialSpanInfo = initialSpanInfoProvider.getInitialSpanInfo(component);
    return new CoreEventComponentTracer(initialSpanInfo, coreEventTracer);
  }

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component, ComponentTracer<?> parentComponentTracer) {
    InitialSpanInfo initialSpanInfo = initialSpanInfoProvider.getInitialSpanInfo(component);
    return new CoreEventComponentTracer(initialSpanInfo, coreEventTracer, parentComponentTracer);
  }

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component, String suffix) {
    InitialSpanInfo initialSpanInfo = initialSpanInfoProvider.getInitialSpanInfo(component, suffix);
    return new CoreEventComponentTracer(initialSpanInfo, coreEventTracer);
  }

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component, String overriddenName, String suffix) {
    InitialSpanInfo initialSpanInfo = initialSpanInfoProvider.getInitialSpanInfo(component, overriddenName, suffix);
    return new CoreEventComponentTracer(initialSpanInfo, coreEventTracer);
  }

}
