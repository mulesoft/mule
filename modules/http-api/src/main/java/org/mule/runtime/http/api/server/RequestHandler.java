/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static java.lang.Thread.currentThread;

import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;

/**
 * Handler for an incoming HTTP request that allows to send the HTTP response asynchronously.
 *
 * @since 4.0
 */
public interface RequestHandler {

  /**
   * Called to handle an incoming HTTP request
   *
   * @param requestContext   HTTP request content
   * @param responseCallback callback to call when the response content is ready.
   */
  void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback);

  /**
   * @return the classloader for the artifact that owns this {@link RequestHandler}.
   *
   * @since 4.1.5
   */
  default ClassLoader getContextClassLoader() {
    return currentThread().getContextClassLoader();
  }

}
