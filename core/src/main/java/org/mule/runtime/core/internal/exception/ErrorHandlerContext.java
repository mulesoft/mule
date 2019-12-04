/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Holds all the context information for a {@code flow} or {@code try} error handler to function.
 *
 * @since 4.3.0
 */
public class ErrorHandlerContext {

  /**
   * The key under which an instance of this class is stored as an internal parameter in a {@link InternalEvent}
   */
  public static final String ERROR_HANDLER_CONTEXT = "error.context";

  /**
   * Extracts an instance stored as an internal parameter in the given {@code result} under the {@link #ERROR_HANDLER_CONTEXT} key
   *
   * @param event
   * @return an {@link ErrorHandlerContext} or {@code null} if none was set on the event
   */
  public static ErrorHandlerContext from(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(ERROR_HANDLER_CONTEXT);
  }

  private final Map<String, ErrorHandlerContextItem> items = new SmallMap<>();

  public void configure(String parameterId, Exception exception, CoreEvent originalEvent,
                        Consumer<CoreEvent> successCallback, Consumer<Throwable> errorCallback) {
    items.put(parameterId, new ErrorHandlerContextItem(exception, originalEvent, successCallback, errorCallback));
  }

  public Exception getException(String parameterId) {
    return items.get(parameterId).exception;
  }

  public CoreEvent getOriginalEvent(String parameterId) {
    return items.get(parameterId).originalEvent;
  }

  public Consumer<CoreEvent> getSuccessCallback(String parameterId) {
    return items.get(parameterId).successCallback;
  }

  public Consumer<Throwable> getRethrowCallback(String parameterId) {
    return items.get(parameterId).errorCallback;
  }

  private static class ErrorHandlerContextItem {

    private final Exception exception;
    private final CoreEvent originalEvent;
    private final Consumer<CoreEvent> successCallback;
    private final Consumer<Throwable> errorCallback;

    public ErrorHandlerContextItem(Exception exception, CoreEvent originalEvent,
                                   Consumer<CoreEvent> successCallback, Consumer<Throwable> errorCallback) {
      this.exception = exception;
      this.originalEvent = originalEvent;
      this.successCallback = successCallback;
      this.errorCallback = errorCallback;
    }
  }

}
