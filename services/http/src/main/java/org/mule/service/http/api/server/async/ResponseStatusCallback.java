/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.server.async;

public interface ResponseStatusCallback {

  /**
   * method to process a failure while sending the response to the client
   *
   * @param throwable exception thrown while sending the response
   */
  void responseSendFailure(Throwable throwable);

  /**
   * notifies that the response was successfully send.
   */
  void responseSendSuccessfully();

}
