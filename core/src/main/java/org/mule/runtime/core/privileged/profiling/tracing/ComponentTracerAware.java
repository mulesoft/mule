/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
