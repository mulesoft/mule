/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.module.oauth2.api.RequestAuthenticationException;
import org.mule.module.oauth2.internal.AbstractGrantType;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;

/**
 * Authorization element for client credentials oauth grant type
 */
public class ClientCredentialsGrantType extends AbstractGrantType implements Initialisable, Startable, MuleContextAware
{

    private ClientCredentialsTokenRequestHandler tokenRequestHandler;
    private MuleContext muleContext;

    public void setTokenRequestHandler(final ClientCredentialsTokenRequestHandler tokenRequestHandler)
    {
        this.tokenRequestHandler = tokenRequestHandler;
    }

    @Override
    public void start() throws MuleException
    {
        tokenRequestHandler.refreshAccessToken();
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
        final String accessToken = tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID).getAccessToken();
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
