/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server.ws;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.message.MessageWithHeaders;
import org.mule.runtime.http.api.domain.request.ClientConnection;
import org.mule.runtime.http.api.domain.request.ServerConnection;
import org.mule.runtime.http.api.ws.WebSocket;
import org.mule.runtime.http.api.ws.WebSocketProtocol;

import java.net.URI;

/**
 * Represents an HTTP request with an upgrade header to initiate a {@link WebSocket}
 *
 * @since 4.2.0
 */
@NoImplement
public interface WebSocketRequest extends MessageWithHeaders {

  /**
   * @return The request path
   */
  String getPath();

  /**
   * @return The request {@link MediaType}
   */
  MediaType getContentType();

  /**
   * @return The query params
   */
  MultiMap<String, String> getQueryParams();

  /**
   * @return The scheme
   */
  WebSocketProtocol getScheme();

  /**
   * @return The protocol version of the HTTP request
   */
  String getHttpVersion();

  /**
   * @return The HTTP request method
   */
  String getMethod();

  /**
   * @return The request {@link URI}
   */
  URI getRequestUri();

  /**
   * @return the server connection descriptor
   */
  ServerConnection getServerConnection();

  /**
   * @return the client connection descriptor
   */
  ClientConnection getClientConnection();
}
