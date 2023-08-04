/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.component;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.profiling.tracing.Span;

/**
 * Component Tracer Factory for creating {@link ComponentTracer}s.
 *
 * @since 4.5.0
 */
public interface ComponentTracerFactory<T extends Event> {

  /**
   * Creates the {@link ComponentTracer} from the {@link Component} processor that will start a {@link Span}.
   *
   * @param component the {@link Component} processor.
   * @return the {@link ComponentTracer} generated from the {@link Component}.
   */
  ComponentTracer<T> fromComponent(Component component);

  /**
   * Creates the {@link ComponentTracer} from the {@link Component} processor that will start a {@link Span}.
   *
   * @param component             the {@link Component} processor.
   * @param parentComponentTracer the {@link ComponentTracer} of the parent {@link Component} processor.
   * @return the {@link ComponentTracer} generated from the {@link Component}.
   */
  ComponentTracer<T> fromComponent(Component component, ComponentTracer<?> parentComponentTracer);

  /**
   * Creates the {@link ComponentTracer} from the {@link Component} processor that will start a {@link Span}.
   *
   * @param component the {@link Component} processor.
   * @param suffix    the suffix of the {@link Component} name.
   * @return the {@link ComponentTracer} generated from the {@link Component}.
   */
  ComponentTracer<T> fromComponent(Component component, String suffix);

  /**
   * Creates the {@link ComponentTracer} from the {@link Component} processor that will start a {@link Span}.
   *
   * @param component      the {@link Component} processor.
   * @param overriddenName the overridden name of the {@link Component} name.
   * @param suffix         the suffix of the {@link Component} name.
   * @return the {@link ComponentTracer} generated from the {@link Component}.
   */
  ComponentTracer<T> fromComponent(Component component, String overriddenName, String suffix);

}
