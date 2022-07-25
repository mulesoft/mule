/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;

/**
 * A visitor for the a {@link InternalSpan}.
 *
 * @param <T> the retunr type
 */
public interface InternalSpanVisitor<T> {

  /**
   * @param exportOnEndSpan the exportOnEndSpan to accept.
   *
   * @return the result.
   */
  T accept(ExportOnEndSpan exportOnEndSpan);

  /**
   *
   * @param executionSpan the executionSpan to accept
   * @return the result.
   */
  T accept(ExecutionSpan executionSpan);

  /**
   * @param spanInternalWrapper the spanInternal
   * @return
   */
  T accept(InternalSpan.SpanInternalWrapper spanInternalWrapper);
}
