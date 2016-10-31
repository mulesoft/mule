/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.oauth2.internal.authorizationcode;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.http.api.listener.HttpListenerConfig;
import org.mule.runtime.module.oauth2.internal.ApplicationCredentials;
import org.mule.runtime.module.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.util.AttributeEvaluator;

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
   * @return expression to determine if a call to the resource secured with oauth failed because the access token has expired or
   *         was revoked.
   */
  String getRefreshTokenWhen();

  /**
   * @return the expression or static value of a certain user authenticated through this config. By being an expression we allow
   *         to authenticate several users and hold state (access token, refresh token, etc) for all those users. This expression
   *         is used during the local authorization url call to determine the resource owner id.
   */
  AttributeEvaluator getLocalAuthorizationUrlResourceOwnerIdEvaluator();

  /**
   * @return the expression or static value of a certain user authenticated through this config. By being an expression we allow
   *         to authenticate several users and hold state (access token, refresh token, etc) for all those users. This expressions
   *         is used during http:request execution to determine the resource owner id.
   */
  AttributeEvaluator getResourceOwnerIdEvaluator();

  /**
   * Does a refresh token for a particular oauth context id.
   *
   * @param currentFlowEvent event from the flow that requires a new access token.
   * @param resourceOwnerId the id of the oauth context to refresh.
   */
  void refreshToken(Event currentFlowEvent, String resourceOwnerId) throws MuleException;

  /**
   * @return the oauth context holder for all the resource owners authenticated in this config.
   */
  ConfigOAuthContext getUserOAuthContext();

  /**
   * @return the tls configuration to use for listening and sending http request
   */
  TlsContextFactory getTlsContext();

}
