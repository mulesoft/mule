/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.internal.message.InternalEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;


/**
 * Holds context information that is stored as an {@link InternalEvent} internal parameter
 * and is used by OnError handlers to handle an {@link org.mule.runtime.api.message.Error}.
 * @see org.mule.runtime.core.api.exception.FlowExceptionHandler
 * @see org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler
 * @since 4.3.0
 */
public class ErrorHandlerContextManager {

  /**
   * The key that can be used to obtain the {@link ErrorHandlerContextManager} from an {@link InternalEvent}.
   * @see {@link InternalEvent#getInternalParameter(String)}
   */
  public static final String ERROR_HANDLER_CONTEXT = "error.context";
  private final Map<String, Deque<ErrorHandlerContext>> items = new SmallMap<>();

  /**
   * Extracts an instance stored as an internal parameter in the given {@code result} under the {@link #ERROR_HANDLER_CONTEXT} key
   *
   * @param event
   * @return an {@link ErrorHandlerContextManager} or {@code null} if none was set on the event
   */
  private static ErrorHandlerContextManager from(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(ERROR_HANDLER_CONTEXT);
  }

  public static ErrorHandlerContext from(FlowExceptionHandler handler, CoreEvent coreEvent) {
    return from(coreEvent).items.get(getParameterId(coreEvent, handler)).peekFirst();
  }

  public static CoreEvent addContext(FlowExceptionHandler handler, MessagingException exception, CoreEvent originalEvent,
                                     Consumer<CoreEvent> successCallback, Consumer<Throwable> errorCallback) {
    ErrorHandlerContextManager errorHandlerContextManager = ErrorHandlerContextManager.from(originalEvent);
    if (errorHandlerContextManager == null) {
      errorHandlerContextManager = new ErrorHandlerContextManager();
      originalEvent = quickCopy(originalEvent, of(ERROR_HANDLER_CONTEXT, errorHandlerContextManager));
    }
    errorHandlerContextManager.items.getOrDefault(getParameterId(originalEvent, handler), new ArrayDeque<>(1))
        .addFirst(new ErrorHandlerContext(exception, originalEvent, successCallback, errorCallback));
    return originalEvent;
  }

  public static void resolveHandling(CoreEvent result, FlowExceptionHandler handler) {
    ErrorHandlerContext errorHandlerContext = from(result).items.get(getParameterId(result, handler)).peekFirst();
    MessagingException exception = errorHandlerContext.getException();
    if (exception.handled()) {
      errorHandlerContext.getSuccessCallback().accept(result);
    } else {
      if (exception.getEvent() != result) {
        exception.setProcessedEvent(result);
      }
      errorHandlerContext.getErrorCallback().accept(exception);
    }
  }

  public static class ErrorHandlerContext {

    private final MessagingException exception;
    private final CoreEvent originalEvent;
    private final Consumer<CoreEvent> successCallback;
    private final Consumer<Throwable> errorCallback;

    public ErrorHandlerContext(MessagingException exception, CoreEvent originalEvent,
                               Consumer<CoreEvent> successCallback, Consumer<Throwable> errorCallback) {
      this.exception = exception;
      this.originalEvent = originalEvent;
      this.successCallback = successCallback;
      this.errorCallback = errorCallback;
    }

    public MessagingException getException() {
      return exception;
    }

    public CoreEvent getOriginalEvent() {
      return originalEvent;
    }

    public Consumer<CoreEvent> getSuccessCallback() {
      return successCallback;
    }

    public Consumer<Throwable> getErrorCallback() {
      return errorCallback;
    }

  }

  private static String getParameterId(CoreEvent event, FlowExceptionHandler handler) {
    final String id = event.getContext().getId();
    return (id != null ? id : "(null)").concat("_").concat(handler.toString());
  }

}
