/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.profiling.tracing;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.tracer.api.component.ComponentTracer;

/**
 * Interface which allows classes to set a {@link ComponentTracer}
 *
 * @since 4.5.0
 */
public interface ComponentTracerAware<T extends Event> {

  void setComponentTracer(ComponentTracer<T> componentTracer);
}
