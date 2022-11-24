/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.tracer.api.context.SpanContext;

import javax.annotation.Nullable;

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
  @Nullable
  SpanContext get(T carrier);
}
