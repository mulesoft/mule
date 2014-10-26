/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.listener.HttpListenerConfig;
import org.mule.module.oauth2.internal.state.ConfigOAuthState;

/**
 * Provides access to the general configuration of an authorization code oauth config.
 */
public interface AuthorizationCodeGrantType
{

    /**
     * @return the name of the oauth config
     */
    String getConfigName();

    /**
     * @return oauth client secret of the hosted application
     */
    String getClientSecret();

    /**
     * @return oauth client id of the hosted application
     */
    String getClientId();

    /**
     * @return redirect url as defined in the oauth authentication server.
     */
    String getRedirectionUrl();

    /**
     * @return expression to determine if a call to the resource secured with oauth failed because the access token has expired or was revoked.
     */
    String getRefreshTokenWhen();

    /**
     * @return the expression or static value of a certain user authenticated through this config. By being an expression we allow to
     * authenticate several users and hold state (access token, refresh token, etc) for all those users.
     */
    String getOAuthStateId();

    /**
     * @return the http request config to use for do the call to the oauth authentication server.
     */
    HttpRequestConfig getRequestConfig();

    /**
     * @return the http listener config to use for receiving request from the user or the oauth authentication server.
     */
    HttpListenerConfig getListenerConfig();

    /**
     * Does a refresh token for a particular oauth state id.
     *
     * @param currentFlowEvent event from the flow that requires a new access token.
     * @param oauthStateId     the id of the oauth state to refresh.
     */
    void refreshToken(MuleEvent currentFlowEvent, String oauthStateId) throws MuleException;

    /**
     * @return the oauth state holder for all the users authenticated in this config.
     */
    ConfigOAuthState getOAuthState();

}
