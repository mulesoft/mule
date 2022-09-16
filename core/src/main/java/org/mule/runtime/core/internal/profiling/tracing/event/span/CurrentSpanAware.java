/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingConditionNotMetException;

import java.util.Map;
import java.util.Optional;

/**
 * A component that has a current {@link org.mule.runtime.api.profiling.tracing.Span}.
 *
 * @since 4.5.0
 */
public interface CurrentSpanAware {

  /**
   * @param span             set the current {@link InternalSpan}
   * @param tracingCondition the tracing condition to assert on setting the current span
   *
   * @throws TracingConditionNotMetException indicates that the condition is not met.
   */
  void setCurrentSpan(InternalSpan span, TracingCondition tracingCondition);

  /**
   * @return the owned {@link InternalSpan}
   */
  Optional<InternalSpan> getCurrentSpan();

  void addCurrentSpanAttributes(Map<String, String> attributes);

  void setCurrentSpanName(String name);

  void addCurrentSpanAttribute(String key, String value);
}
