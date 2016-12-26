/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import org.mule.extension.http.internal.listener.server.HttpListenerConfig;
import org.mule.extension.oauth2.internal.ApplicationCredentials;
import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.service.http.api.server.HttpServer;

/**
 * Provides access to the general configuration of an authorization code oauth config.
 */
public interface AuthorizationCodeGrantType extends ApplicationCredentials {

  /**
   * @return callback listener configuration to start the server and define corresponding flow.
   */
  HttpListenerConfig getLocalCallbackConfig();

  /**
   * @return callback listener path for the flow to be created according to localCallbackConfig.
   */
  String getLocalCallbackConfigPath();

  /**
   * @return local callback URL for the flow to be created.
   */
  String getLocalCallbackUrl();

  /**
   * @return the external address of the callback, sent to the client.
   */
  String getExternalCallbackUrl();

  /**
   * @return the oauth context holder for all the resource owners authenticated in this config.
   */
  ConfigOAuthContext getUserOAuthContext();

  /**
   * @return the tls configuration to use for listening and sending http request
   */
  TlsContextFactory getTlsContext();

  /**
   * @return the http server to listen for requests during the OAuth dance.
   */
  HttpServer getServer();

}
