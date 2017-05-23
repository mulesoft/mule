/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.async;

import org.mule.runtime.http.api.domain.message.response.HttpResponse;

/**
 * Handler for receiving an HTTP response asynchronously.
 *
 * @since 4.0
 */
public interface ResponseHandler {

  /**
   * Invoked on successful completion of asynchronous processing of an {@link HttpResponse}.
   * <p>
   * Exceptions found while processing the {@code response} are to be notified through
   * the {@code exceptionCallback}, which might (depending on {@code HandledCompletionExceptionResult})
   * produce a new value as the result of handling such error
   *
   * @param response            the result of processing
   */
  void onCompletion(HttpResponse response);

  /**
   * Invoked when a failure occurs during asynchronous processing of an {@link HttpResponse}.
   *
   * @param exception the exception thrown during processing
   */
  void onFailure(Exception exception);

}
