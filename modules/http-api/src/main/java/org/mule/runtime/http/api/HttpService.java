/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientFactory;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerFactory;
import org.mule.runtime.http.api.ws.WebSocketBroadcaster;
import org.mule.runtime.http.api.utils.RequestMatcherRegistry;
import org.mule.runtime.http.api.utils.RequestMatcherRegistry.RequestMatcherRegistryBuilder;

/**
 * Provides HTTP server and client factories.
 *
 * @since 4.0
 */
public interface HttpService extends Service {

  /**
   * @return an {@link HttpServerFactory} capable of creating {@link HttpServer}s.
   */
  HttpServerFactory getServerFactory();

  /**
   * @return an {@link HttpClientFactory} capable of creating {@link HttpClient}s.
   */
  HttpClientFactory getClientFactory();

  /**
   * @return a fresh builder of {@link RequestMatcherRegistry RequestMatcherRegistries}.
   * @since 4.1.5
   */
  @Experimental
  RequestMatcherRegistryBuilder getRequestMatcherRegistryBuilder();

  @Experimental
  default WebSocketBroadcaster newWebSocketBroadcaster() {
    throw new UnsupportedOperationException("WebSockets are only supported in Enterprise Edition");
  }
}
