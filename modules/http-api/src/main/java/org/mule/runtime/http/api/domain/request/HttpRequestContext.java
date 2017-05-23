/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.request;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.server.HttpServer;

/**
 * Representation of all HTTP request data concerning an {@link HttpServer}.
 *
 * @since 4.0
 */
public interface HttpRequestContext {

  /**
   * @return the HTTP request message
   */
  HttpRequest getRequest();

  /**
   * @return the client connection descriptor
   */
  ClientConnection getClientConnection();

  /**
   * @return The scheme of the HTTP request URL (http or https)
   */
  String getScheme();

}
