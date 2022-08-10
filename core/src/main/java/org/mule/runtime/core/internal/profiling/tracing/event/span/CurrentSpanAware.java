/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;

import java.util.Optional;

/**
 * A component that has a current {@link org.mule.runtime.api.profiling.tracing.Span}.
 *
 * @since 4.5.0
 */
public interface CurrentSpanAware {

  /**
   * @param span set the current {@link InternalSpan}
   */
  void setCurrentSpan(InternalSpan span);

  /**
   * @return the owned {@link InternalSpan}
   */
  Optional<InternalSpan> getCurrentSpan();
}
