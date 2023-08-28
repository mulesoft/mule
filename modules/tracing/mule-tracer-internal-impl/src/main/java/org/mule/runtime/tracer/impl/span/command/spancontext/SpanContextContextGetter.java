/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command.spancontext;

import org.mule.runtime.tracer.api.context.SpanContext;

/**
 * A getter for the distributed trace context.
 *
 * @param <T> carrier
 */
public interface SpanContextContextGetter<T> {

  /**
   * @param carrier the carrier that has the {@link SpanContext}.
   *
   * @return the {@link SpanContext}.
   */
  SpanContext get(T carrier);
}
