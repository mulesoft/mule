/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.error;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.InternalSpanCallStack;

public class DefaultSpanError implements InternalSpanError {

  private final Error error;
  private final InternalSpanCallStack errorStackTrace;
  private final boolean isEscapingSpan;

  public DefaultSpanError(Error error, InternalSpanCallStack errorStackTrace, boolean isEscapingSpan) {
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
    return errorStackTrace;
  }

}
