/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server.async;

/**
 * Callback to notify of success or failure sending a response.
 *
 * @since 4.0
 */
public interface ResponseStatusCallback {

  /**
   * Method to process a failure while sending the response to the client
   *
   * @param throwable exception thrown while sending the response
   */
  void responseSendFailure(Throwable throwable);

  /**
   * Notifies that the response was successfully send.
   */
  void responseSendSuccessfully();

  /**
   * Method to process an error without sending a response, since the error that occurred cannot be recovered from. Implementors
   * should override this particularly when dealing with SDK asynchronous sources that require a completion callback notification.
   *
   * @param throwable exception thrown while sending the response
   * @since 4.1.6
   */
  default void onErrorSendingResponse(Throwable throwable) {}
}
