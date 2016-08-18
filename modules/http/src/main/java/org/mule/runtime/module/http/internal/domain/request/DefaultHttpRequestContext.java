/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.domain.request;

import org.mule.service.http.api.domain.request.HttpRequest;
import org.mule.service.http.api.domain.request.HttpRequestContext;

/**
 * Holds the input from an http request.
 */
public class DefaultHttpRequestContext implements HttpRequestContext {

  private final DefaultClientConnection clientConnection;
  private HttpRequest request;
  private String scheme;

  public DefaultHttpRequestContext(HttpRequest httpRequest, DefaultClientConnection clientConnection, String scheme) {
    this.request = httpRequest;
    this.clientConnection = clientConnection;
    this.scheme = scheme;
  }

  /**
   * @return the http request content
   */
  public HttpRequest getRequest() {
    return this.request;
  }

  /**
   * @return client connection descriptor
   */
  public DefaultClientConnection getClientConnection() {
    return clientConnection;
  }

  /**
   * @return The scheme of the HTTP request URL (http or https)
   */
  public String getScheme() {
    return scheme;
  }
}
