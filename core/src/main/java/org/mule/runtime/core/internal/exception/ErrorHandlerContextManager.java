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
 * Manages context information. If present, an instance of this class is stored as an {@link InternalEvent} internal parameter,
 * under the {@link ErrorHandlerContextManager#ERROR_HANDLER_CONTEXT} key.
 * It's {@link ErrorHandlerContext} instances are created and used by OnError handlers when handling a {@link MessagingException}.
 * @see org.mule.runtime.core.api.exception.FlowExceptionHandler
 * @see org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler
 * @since 4.3.0
 */
public class ErrorHandlerContextManager {

  /**
   * The key used to store a {@link ErrorHandlerContextManager} as an {@link InternalEvent} internal parameter.
   * @see {@link InternalEvent#getInternalParameter(String)}
   */
  public static final String ERROR_HANDLER_CONTEXT = "error.context";
  private final Map<String, Deque<ErrorHandlerContext>> items = new SmallMap<>();

  /**
   * Extracts an {@link ErrorHandlerContextManager} instance from the given {@link CoreEvent}.
   * @param event The given {@link CoreEvent}.
   * @return an {@link ErrorHandlerContextManager} or {@code null} if none was set on the event.
   */
  private static ErrorHandlerContextManager from(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(ERROR_HANDLER_CONTEXT);
  }

  /**
   * <p>Used by OnError handlers to obtain it's current {@link ErrorHandlerContext}.
   * @param handler The onError handler making this call.</p>
   * <p>Note that {@link #addContext(FlowExceptionHandler, MessagingException, Consumer, Consumer)}
   * must be previously invoked.</p>
   * @param coreEvent The {@link CoreEvent} that the onError handler is processing.
   * @return An {@link ErrorHandlerContext} instance.
   */
  public static ErrorHandlerContext from(FlowExceptionHandler handler, CoreEvent coreEvent) {
    return from(coreEvent).items.get(getParameterId(coreEvent, handler)).peekFirst();
  }

  /**
   * Used by OnError handlers to store its {@link ErrorHandlerContext} when handling a {@link MessagingException}.
   * @param handler The OnError handler invoking this method.
   * @param exception The {@link MessagingException} being handled.
   * @param successCallback Callback that will be invoked if the exception is successfully handled by the OnError handler.
   * @param errorCallback Callback that will be invoked if the exception is could not be handled by the OnError handler.
   * @return The {@link CoreEvent} whose processing caused the {@link MessagingException}.
   * @see #resolveHandling(FlowExceptionHandler, CoreEvent)
   * @see #resolveHandling(FlowExceptionHandler, MessagingException)
   */
  public static CoreEvent addContext(FlowExceptionHandler handler, MessagingException exception,
                                     Consumer<CoreEvent> successCallback, Consumer<Throwable> errorCallback) {
    CoreEvent originalEvent = exception.getEvent();
    ErrorHandlerContextManager errorHandlerContextManager = ErrorHandlerContextManager.from(exception.getEvent());
    if (errorHandlerContextManager == null) {
      errorHandlerContextManager = new ErrorHandlerContextManager();
      originalEvent = quickCopy(originalEvent, of(ERROR_HANDLER_CONTEXT, errorHandlerContextManager));
    }
    errorHandlerContextManager.items.computeIfAbsent(getParameterId(originalEvent, handler), key -> new ArrayDeque<>(1))
        .addFirst(new ErrorHandlerContext(exception, originalEvent, successCallback, errorCallback));
    return originalEvent;
  }

  /**
   * <p>Used by OnError handlers to communicate the result of a successful (without errors) {@link MessagingException} handling.</p>
   * <p>Note that {@link #addContext(FlowExceptionHandler, MessagingException, Consumer, Consumer)}
   * must be previously invoked.</p>
   * @param handler The OnErrorHandler invoking this method.
   * @param result The {@link CoreEvent} resulting from the handling.
   */
  public static void resolveHandling(FlowExceptionHandler handler, CoreEvent result) {
    ErrorHandlerContext errorHandlerContext = from(result).items.get(getParameterId(result, handler)).removeFirst();
    MessagingException exception = errorHandlerContext.getException();
    // A successful handling event does not imply that the exception was handled, but that there were no errors during the handling.
    if (exception.handled()) {
      errorHandlerContext.successCallback.accept(result);
    } else {
      if (exception.getEvent() != result) {
        exception.setProcessedEvent(result);
      }
      errorHandlerContext.errorCallback.accept(exception);
    }
  }

  /**
   * <p>Used by OnError handlers to communicate the result of an unsuccessful {@link MessagingException} handling attempt.</p>
   * <p>Note that {@link #addContext(FlowExceptionHandler, MessagingException, Consumer, Consumer)}
   * must be previously invoked.</p>
   * @param handler The OnErrorHandler invoking this method.
   * @param exception The {@link MessagingException} resultant from the handling attempt.
   */
  public static void resolveHandling(FlowExceptionHandler handler, MessagingException exception) {
    ErrorHandlerContext errorHandlerContext =
        from(exception.getEvent()).items.get(getParameterId(exception.getEvent(), handler)).removeFirst();
    errorHandlerContext.errorCallback.accept(exception);
  }

  /**
   * Constructs the key that will be used to store a {@link ErrorHandlerContextManager} instance
   * as an {@link CoreEvent} internal parameter.
   * @param event The {@link CoreEvent} where the {@link ErrorHandlerContextManager} instance will be stored.
   * @param handler The OnError handler requesting an {@link ErrorHandlerContext} storage.
   * @return The key that will be used to store a {@link ErrorHandlerContextManager} instance.
   * @see #addContext(FlowExceptionHandler, MessagingException, Consumer, Consumer)
   */
  private static String getParameterId(CoreEvent event, FlowExceptionHandler handler) {
    final String id = event.getContext().getId();
    return (id != null ? id : "(null)").concat("_").concat(handler.toString());
  }

  /**
   * Holds the context data used by OnError handlers when handling a {@link MessagingException}.
   */
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
  }
}
