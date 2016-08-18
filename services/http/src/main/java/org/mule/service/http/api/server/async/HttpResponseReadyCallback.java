/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.server.async;

import org.mule.service.http.api.domain.response.HttpResponse;

/**
 * Handler for sending an http response asynchronously
 */
public interface HttpResponseReadyCallback {

  /**
   * method to send response to the client
   *
   * @param response http response content
   * @param responseStatusCallback callback to be called if there's a failure while sending the response
   */
  void responseReady(HttpResponse response, ResponseStatusCallback responseStatusCallback);

}
