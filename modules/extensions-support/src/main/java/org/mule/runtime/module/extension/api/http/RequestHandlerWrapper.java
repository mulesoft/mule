/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.RequestHandler;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;

public class RequestHandlerWrapper implements RequestHandler {

  private final org.mule.sdk.api.http.server.RequestHandler delegate;

  public RequestHandlerWrapper(org.mule.sdk.api.http.server.RequestHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
    // TODO: Null?
    delegate.handleRequest(null, null);
  }

  @Override
  public ClassLoader getContextClassLoader() {
    return delegate.getContextClassLoader();
  }
}
