package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;

public interface InternalSpanError extends SpanError {

  FlowCallStack getErrorStacktrace();

  static InternalSpanError getInternalSpanError(SpanError apiSpanError) {
    if (apiSpanError instanceof InternalSpanError) {
      return (InternalSpanError) apiSpanError;
    } else {
      throw new UnsupportedOperationException(String.format("apiSpanError is not an [%s] but a [%s]",
                                                            InternalSpanError.class.getName(),
                                                            apiSpanError.getClass().getName()));
    }
  }

  static InternalSpanError getInternalSpanError(CoreEvent coreEvent, boolean isErrorEscapingSpan) {
    Error spanError = coreEvent.getError()
        .orElseThrow(() -> new IllegalArgumentException(String.format("Provided coreEvent [%s] does not declare an error.",
                                                                      coreEvent)));
    return new DefaultSpanError(spanError, coreEvent.getFlowCallStack(), isErrorEscapingSpan);
  }

}
