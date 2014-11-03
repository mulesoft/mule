/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.listener.HttpListenerConfig;
import org.mule.module.oauth2.internal.ClientApplicationCredentials;
import org.mule.module.oauth2.internal.authorizationcode.state.ConfigOAuthContext;

/**
 * Provides access to the general configuration of an authorization code oauth config.
 */
public interface AuthorizationCodeGrantType extends ClientApplicationCredentials
{

    /**
     * @return the name of the oauth config
     */
    String getConfigName();

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
    String getResourceOwnerId();

    /**
     * @return the http request config to use for do the call to the oauth authentication server.
     */
    HttpRequestConfig getRequestConfig();

    /**
     * @return the http listener config to use for receiving request from the user or the oauth authentication server.
     */
    HttpListenerConfig getListenerConfig();

    /**
     * Does a refresh token for a particular oauth context id.
     *
     * @param currentFlowEvent event from the flow that requires a new access token.
     * @param resourceOwnerId the id of the oauth context to refresh.
     */
    void refreshToken(MuleEvent currentFlowEvent, String resourceOwnerId) throws MuleException;

    /**
     * @return the oauth context holder for all the resource owners authenticated in this config.
     */
    ConfigOAuthContext getUserOAuthContext();

}
