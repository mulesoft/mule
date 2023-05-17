package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.tracer.api.EventTracer;

public class OperationExecutionTraceCallback implements ExecutorCallback {

    private final EventedExecutionContext<?> eventedExecutionContext;
    private final EventTracer<CoreEvent> coreEventEventTracer;

    public OperationExecutionTraceCallback(EventedExecutionContext<?> eventedExecutionContext, EventTracer<CoreEvent> coreEventEventTracer) {
        this.eventedExecutionContext = eventedExecutionContext;
        this.coreEventEventTracer = coreEventEventTracer;
    }

    @Override
    public void complete(Object value) {
        coreEventEventTracer.endCurrentSpan(eventedExecutionContext.getEvent());
    }

    @Override
    public void error(Throwable e) {
        // TODO: Add the ticket ID for the DEBUG level errors task.
        coreEventEventTracer.endCurrentSpan(eventedExecutionContext.getEvent());
    }
}
