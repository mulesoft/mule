/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.registry.RegistrationException;
import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.listener.HttpListenerConfig;
import org.mule.module.oauth2.internal.state.ConfigOAuthState;
import org.mule.module.oauth2.internal.state.OAuthStateRegistry;

/**
 * Represents the config element for oauth:authentication-code-config.
 * <p/>
 * This config will:
 * - If the authorization-request is defined then it will create a flow listening for an user call to begin the oauth login.
 * - If the token-request is defined then it will create a flow for listening in the redirect uri so we can get the authentication code and retrieve the access token
 */
public class AuthorizationCodeConfig implements Initialisable, Disposable, Startable, AuthorizationCodeGrantType, MuleContextAware
{

    private String name;
    private String clientId;
    private String clientSecret;
    private String redirectionUrl;
    private String oauthStateId;
    private HttpRequestConfig requestConfig;
    private HttpListenerConfig listenerConfig;
    private AuthorizationRequestHandler authorizationRequestHandler;
    private AbstractTokenRequestHandler tokenRequestHandler;
    private ConfigOAuthState configOAuthState;
    private MuleContext muleContext;
    private OAuthStateRegistry oauthStateRegistry;

    public void setName(final String name)
    {
        this.name = name;
    }

    public void setClientId(final String clientId)
    {
        this.clientId = clientId;
    }

    public void setClientSecret(final String clientSecret)
    {
        this.clientSecret = clientSecret;
    }

    public void setRedirectionUrl(final String redirectionUrl)
    {
        this.redirectionUrl = redirectionUrl;
    }

    public void setAuthorizationRequestHandler(final AuthorizationRequestHandler authorizationRequestHandler)
    {
        this.authorizationRequestHandler = authorizationRequestHandler;
    }

    public void setTokenRequestHandler(final AbstractTokenRequestHandler tokenRequestHandler)
    {
        this.tokenRequestHandler = tokenRequestHandler;
    }

    public void setRequestConfig(final HttpRequestConfig requestConfig)
    {
        this.requestConfig = requestConfig;
    }

    public void setListenerConfig(final HttpListenerConfig listenerConfig)
    {
        this.listenerConfig = listenerConfig;
    }

    @Override
    public void start() throws MuleException
    {
        if (authorizationRequestHandler != null)
        {
            authorizationRequestHandler.setOauthConfig(this);
            authorizationRequestHandler.init();
        }
        if (tokenRequestHandler != null)
        {
            tokenRequestHandler.setOauthConfig(this);
            tokenRequestHandler.init();
        }
    }

    public String getRedirectionUrl()
    {
        return redirectionUrl;
    }

    @Override
    public String getRefreshTokenWhen()
    {
        return tokenRequestHandler.getRefreshTokenWhen();
    }

    @Override
    public String getOAuthStateId()
    {
        return oauthStateId;
    }

    @Override
    public HttpRequestConfig getRequestConfig()
    {
        return requestConfig;
    }

    @Override
    public HttpListenerConfig getListenerConfig()
    {
        return listenerConfig;
    }

    @Override
    public void refreshToken(final MuleEvent currentFlowEvent, final String oauthStateId) throws MuleException
    {
        tokenRequestHandler.refreshToken(currentFlowEvent, oauthStateId);
    }

    @Override
    public ConfigOAuthState getOAuthState()
    {
        return configOAuthState;
    }

    @Override
    public String getConfigName()
    {
        return name;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setOauthStateId(String oauthStateId)
    {
        this.oauthStateId = oauthStateId;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            configOAuthState = new ConfigOAuthState(muleContext.getLockFactory(), getConfigName());
            oauthStateRegistry = this.muleContext.getRegistry().lookupObject(OAuthStateRegistry.class);
            oauthStateRegistry.registerOAuthState(getConfigName(), configOAuthState);
        }
        catch (RegistrationException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void dispose()
    {
        oauthStateRegistry.unregisterOAuthState(getConfigName());
    }
}
