/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.client;

import org.mule.service.http.api.domain.request.HttpRequest;
import org.mule.service.http.api.domain.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Object that sends an HTTP request, and returns the response.
 *
 * @since 4.0
 */
public interface HttpClient {

  void start();

  void stop();

  /**
   * Sends a HttpRequest blocking the current thread until a response is available for the request times out.
   */
  HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication)
      throws IOException, TimeoutException;
}
