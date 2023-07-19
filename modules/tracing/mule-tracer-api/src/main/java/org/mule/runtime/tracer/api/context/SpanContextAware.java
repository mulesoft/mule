/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.context;

/**
 * A component that carries information about the distributed trace context
 *
 * @since 4.5.0
 */
public interface SpanContextAware {

  /**
   * @return the {@link SpanContext} associated with the event.
   *
   * @since 4.5.0
   */
  SpanContext getSpanContext();

  /**
   * @param spanContext the distributed trace context to set.
   *
   * @since 4.5.0
   */
  void setSpanContext(SpanContext spanContext);
}
