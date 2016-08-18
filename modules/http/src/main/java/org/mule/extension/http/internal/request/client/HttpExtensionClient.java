/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request.client;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpRequestAuthentication;
import org.mule.service.http.api.domain.request.HttpRequest;
import org.mule.service.http.api.domain.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class HttpExtensionClient implements Startable, Stoppable {

  private final HttpAuthentication authentication;
  private final HttpClient httpClient;
  private final UriParameters uriParameters;

  public HttpExtensionClient(HttpClient httpClient, UriParameters uriParameters, HttpAuthentication authentication) {
    this.httpClient = httpClient;
    this.uriParameters = uriParameters;
    this.authentication = authentication;
  }

  /**
   * Returns the default parameters for the {@link HttpRequest} URI.
   */
  public UriParameters getDefaultUriParameters() {
    return uriParameters;
  }

  public HttpAuthentication getDefaultAuthentication() {
    return authentication;
  }

  @Override
  public void start() throws MuleException {
    httpClient.start();
    startIfNeeded(authentication);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(authentication);
    httpClient.stop();
  }

  public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects,
                           HttpRequestAuthentication authentication)
      throws IOException, TimeoutException {
    return httpClient.send(request, responseTimeout, followRedirects, authentication);
  }
}
