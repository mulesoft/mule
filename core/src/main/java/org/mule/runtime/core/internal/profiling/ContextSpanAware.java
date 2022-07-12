/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import java.util.Optional;

/**
 * A component that has a context {@link org.mule.runtime.api.profiling.tracing.Span}.
 *
 * There are certain spans (unit of works) that are associated to a context. Suppose an event that is dispatched to a flow. Every
 * time it hits an operation, a span may be created for tracing the unit of work associated to that operation. This span will be
 * associated to the {@link org.mule.runtime.api.event.EventContext}: the current span for that event will be the one associated
 * to that operation till the span ends (the operation finishes executing).
 *
 * The distributed trace context in that case is {@link ContextSpanAware} because there is a current
 * {@link org.mule.runtime.api.profiling.tracing.Span} associated to it.
 *
 * @see {@link org.mule.runtime.core.internal.trace.DistributedTraceContext}
 * @see {@link org.mule.runtime.core.internal.event.trace.EventDistributedTraceContext}
 *
 * @since 4.5.0
 */
public interface ContextSpanAware {

  /**
   * @param span set the context current {@link InternalSpan}
   */
  void setContextCurrentSpan(InternalSpan span);

  /**
   * @return the owned {@link InternalSpan}
   */
  Optional<InternalSpan> getContextCurrentSpan();
}
