/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.client;

import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Object that sends an HTTP request, and returns the response.
 *
 * @since 4.0
 */
public interface HttpClient extends Startable, Stoppable {

  /**
   * Returns the default parameters for the {@link HttpRequest} URI.
   */
  UriParameters getDefaultUriParameters();

  /**
   * Returns the default {@link HttpAuthentication} to be used on requests.
   */
  HttpAuthentication getDefaultAuthentication();

  /**
   * Sends a HttpRequest blocking the current thread until a response is available for the request times out.
   */
  HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication)
      throws IOException, TimeoutException;
}
