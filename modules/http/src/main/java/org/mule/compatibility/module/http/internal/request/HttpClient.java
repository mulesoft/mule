/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.http.internal.request;

import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.client.HttpRequestAuthentication;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Object that sends an HTTP request, and returns the response.
 */
public interface HttpClient extends Startable, Stoppable {

  /**
   * Sends a HttpRequest blocking the current thread until a response is available for the request times out.
   */
  HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication)
      throws IOException, TimeoutException;

  /**
   * Sends a HttpRequest without blocking the current thread. When a response is available or the request times out the provided
   * CompletionHandler will be invoked.
   */
  void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication,
            final CompletionHandler<HttpResponse, Exception> handler);

}
