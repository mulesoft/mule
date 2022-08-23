package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;

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
  public FlowCallStack getErrorStacktrace() {
    return errorStackTrace;
  }
}
