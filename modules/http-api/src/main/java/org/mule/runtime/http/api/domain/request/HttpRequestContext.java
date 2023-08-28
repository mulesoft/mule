/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.domain.request;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.server.HttpServer;

/**
 * Representation of all HTTP request data concerning an {@link HttpServer}.
 *
 * @since 4.0
 */
@NoImplement
public interface HttpRequestContext {

  /**
   * @return The scheme of the HTTP request URL (http or https)
   */
  String getScheme();

  /**
   * @return the HTTP request message
   */
  HttpRequest getRequest();

  /**
   * @return the server connection descriptor
   */
  ServerConnection getServerConnection();

  /**
   * @return the client connection descriptor
   */
  ClientConnection getClientConnection();

}
