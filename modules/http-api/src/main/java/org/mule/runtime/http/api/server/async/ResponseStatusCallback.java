/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
