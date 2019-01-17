/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.module.oauth2.api.RequestAuthenticationException;
import org.mule.module.oauth2.internal.AbstractGrantType;
import org.mule.module.oauth2.internal.OAuthTokenMuleException;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;

/**
 * Authorization element for client credentials oauth grant type
 */
public class ClientCredentialsGrantType extends AbstractGrantType implements Initialisable, Disposable, Startable, MuleContextAware
{

    private ClientCredentialsTokenRequestHandler tokenRequestHandler;
    private MuleContext muleContext;
    
    private boolean accessTokenRefreshedOnStart = false;

    public void setTokenRequestHandler(final ClientCredentialsTokenRequestHandler tokenRequestHandler)
    {
        this.tokenRequestHandler = tokenRequestHandler;
    }

    @Override
    public void start() throws MuleException
    {
        try {
            tokenRequestHandler.refreshAccessToken();
            accessTokenRefreshedOnStart = true;
        }
        catch(OAuthTokenMuleException e)
        {
            tokenRequestHandler.dispose();
            throw e;
        }
        catch (MessagingException | DefaultMuleException e)
        {
            // Nothing to do, accessTokenRefreshedOnStart remains false and this is called later
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (tokenManagerConfig == null)
        {
            this.tokenManagerConfig = TokenManagerConfig.createDefault(muleContext);
        }
        tokenRequestHandler.setApplicationCredentials(this);
        tokenRequestHandler.setTokenManager(tokenManagerConfig);
        try
        {
            tokenRequestHandler.buildHttpRequestOptions(tlsContextFactory, proxyConfig);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
        tokenRequestHandler.initialise();
    }

    @Override
    public void dispose()
    {
        tokenRequestHandler.dispose();
    }
    
    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }

    public String getRefreshTokenWhen()
    {
        return tokenRequestHandler.getRefreshTokenWhen();
    }

    public void refreshAccessToken() throws MuleException
    {
        tokenRequestHandler.refreshAccessToken();
    }

    @Override
    public void authenticate(MuleEvent muleEvent, HttpRequestBuilder builder) throws MuleException
    {
        if(!accessTokenRefreshedOnStart) {
            accessTokenRefreshedOnStart = true;
            refreshAccessToken();
        }

        final String accessToken = getAccessToken(tokenManagerConfig.getConfigOAuthContext(), DEFAULT_RESOURCE_OWNER_ID);

        if (accessToken == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("No access token found. Verify that you have authenticated before trying to execute an operation to the API.")));
        }
        builder.addHeader(HttpHeaders.Names.AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
    }

    @Override
    public boolean shouldRetry(final MuleEvent firstAttemptResponseEvent)
    {
        final Object value = muleContext.getExpressionManager().evaluate(getRefreshTokenWhen(), firstAttemptResponseEvent);
        if (!(value instanceof Boolean))
        {
            throw new MuleRuntimeException(createStaticMessage("Expression %s should return a boolean but return %s", getRefreshTokenWhen(), value));
        }
        final Boolean shouldRetryRequest = (Boolean) value;
        if (shouldRetryRequest)
        {
            try
            {
                refreshAccessToken();
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        return shouldRetryRequest;
    }

}
