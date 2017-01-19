/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;
import org.mule.service.http.api.server.HttpServerFactory;


/**
 * @deprecated Use only internal to the dancer
 */
@Deprecated
public interface OAuthHttpListenersServersManager {

  /**
   * Builds or returns an already built {@link HttpServer} wrapper.
   * 
   * @param serverConfiguration the configuration for the new server. Its port will be used to determine if a new one must be
   *        created or an existing one returned.
   * @return the corresponding server wrapper.
   * @throws ConnectionException See {@link HttpServerFactory#create(HttpServerConfiguration)}
   */
  HttpServer getServer(HttpServerConfiguration serverConfiguration) throws ConnectionException;

}
