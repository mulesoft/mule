/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.oauth2.internal.authorizationcode.OAuthAuthenticationHeader.buildAuthorizationHeaderContent;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.module.http.HttpHeaders;
import org.mule.module.http.domain.HttpRequestBuilder;
import org.mule.module.http.request.HttpAuthentication;
import org.mule.module.oauth2.api.RequestAuthenticationException;

public class ClientCredentialsAuthenticate implements HttpAuthentication, MuleContextAware
{

    private MuleContext muleContext;
    private ClientCredentialsConfig config;

    @Override
    public void authenticate(MuleEvent muleEvent, HttpRequestBuilder builder) throws MuleException
    {
        final String accessToken = config.getClientCredentialsStore().getAccessToken();
        if (accessToken == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("No access token found. Verify that you have authenticated before trying to execute an operation to the API.")));
        }
        builder.addHeader(HttpHeaders.Names.AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
    }

    @Override
    public boolean shouldRetry(final MuleEvent firstAttemptResponseEvent)
    {
        final Object value = muleContext.getExpressionManager().evaluate(config.getRefreshTokenWhen(), firstAttemptResponseEvent);
        if (!(value instanceof Boolean))
        {
            throw new MuleRuntimeException(createStaticMessage("Expression %s should return a boolean but return %s", config.getRefreshTokenWhen(), value));
        }
        final Boolean shouldRetryRequest = (Boolean) value;
        if (shouldRetryRequest)
        {
            try
            {
                config.refreshAccessToken();
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        return shouldRetryRequest;
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }

    public void setConfig(final ClientCredentialsConfig config)
    {
        this.config = config;
    }

}
