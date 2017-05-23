/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server.async;

import org.mule.runtime.http.api.domain.message.response.HttpResponse;

/**
 * Handler for sending an HTTP response asynchronously
 *
 * @since 4.0
 */
public interface HttpResponseReadyCallback {

  /**
   * Method to send response to the client.
   *
   * @param response HTTP response content.
   * @param responseStatusCallback callback to be called if there's a failure while sending the response or when it is successfully sent.
   */
  void responseReady(HttpResponse response, ResponseStatusCallback responseStatusCallback);

}
