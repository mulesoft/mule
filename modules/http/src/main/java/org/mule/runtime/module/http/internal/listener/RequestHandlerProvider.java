/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import org.mule.service.http.api.domain.request.HttpRequest;
import org.mule.service.http.api.server.RequestHandler;

/**
 * Provider of {@link RequestHandler} for a certain incoming http request.
 */
public interface RequestHandlerProvider {

  /**
   * Retrieves a RequestHandler to handle the http request
   *
   * @param ip ip address in which the http request was made
   * @param port port in which the http request was made
   * @param request the http request content
   * @return a handler for the request
   */
  RequestHandler getRequestHandler(String ip, int port, HttpRequest request);

}
