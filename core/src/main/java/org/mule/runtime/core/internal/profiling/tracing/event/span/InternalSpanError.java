/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.core.api.context.notification.FlowCallStack;

/**
 * Extends the {@link SpanError} API with mule core internal operations.
 */
public interface InternalSpanError extends SpanError {

  /**
   * @return The call stack trace of the error.
   */
  FlowCallStack getErrorStacktrace();

  /**
   * Converts an API {@link SpanError} into an {@link InternalSpanError}.
   * 
   * @param apiSpanError A {@link SpanError} instance.
   * @return {@link InternalSpanError} instance.
   */
  static InternalSpanError getInternalSpanError(SpanError apiSpanError) {
    if (apiSpanError instanceof InternalSpanError) {
      return (InternalSpanError) apiSpanError;
    } else {
      throw new UnsupportedOperationException(String.format("apiSpanError is not an [%s] but a [%s]",
                                                            InternalSpanError.class.getName(),
                                                            apiSpanError.getClass().getName()));
    }
  }

}
