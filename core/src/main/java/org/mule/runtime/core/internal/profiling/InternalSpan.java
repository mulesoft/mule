/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import java.util.function.Consumer;

/**
 * A {@link InternalSpan }used internally by the runtime. It defines an extension of the span contract that is only used
 * internally.
 *
 * @since 4.5.0
 */
public interface InternalSpan extends Span {

  /**
   * Ends the span.
   */
  void end();
}
