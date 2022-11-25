/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.error;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.InternalSpanCallStack;

public class DefaultSpanError implements InternalSpanError {

  private final Error error;
  private final FlowCallStack errorStackTrace;
  private final boolean isEscapingSpan;

  public DefaultSpanError(Error error, FlowCallStack errorStackTrace, boolean isEscapingSpan) {
    this.error = error;
    this.errorStackTrace = errorStackTrace;
    this.isEscapingSpan = isEscapingSpan;
  }

  @Override
  public Error getError() {
    return error;
  }

  @Override
  public boolean isEscapingSpan() {
    return isEscapingSpan;
  }

  @Override
  public InternalSpanCallStack getErrorStacktrace() {
    return new DefaultSpanCallStack(errorStackTrace);
  }

}
