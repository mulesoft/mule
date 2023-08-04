/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.Optional;

/**
 * A dummy implementation of {@link ComponentTracerFactory}.
 *
 * @since 4.5.0
 */
public class DummyComponentTracerFactory implements ComponentTracerFactory<CoreEvent> {

  private static final DummyComponentTracerFactory INSTANCE = new DummyComponentTracerFactory();

  public static final ComponentTracer<CoreEvent> DUMMY_COMPONENT_TRACER_INSTANCE = new DummyComponentTracer();

  public static DummyComponentTracerFactory getDummyComponentTracerFactory() {
    return INSTANCE;
  }

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component) {
    return DUMMY_COMPONENT_TRACER_INSTANCE;
  }

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component, ComponentTracer<?> parentComponentTracer) {
    return DUMMY_COMPONENT_TRACER_INSTANCE;
  }

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component, String suffix) {
    return DUMMY_COMPONENT_TRACER_INSTANCE;
  }

  @Override
  public ComponentTracer<CoreEvent> fromComponent(Component component, String overriddenName, String suffix) {
    return DUMMY_COMPONENT_TRACER_INSTANCE;
  }

  private static class DummyComponentTracer implements ComponentTracer<CoreEvent> {

    @Override
    public Optional<InternalSpan> startSpan(CoreEvent event) {
      return Optional.empty();
    }

    @Override
    public void endCurrentSpan(CoreEvent event) {
      // Nothing to do.
    }

    @Override
    public void addCurrentSpanAttribute(CoreEvent event, String key, String value) {
      // Nothing to do.
    }

    @Override
    public Assertion getComponentSpanAssertion() {
      return null;
    }
  }
}
