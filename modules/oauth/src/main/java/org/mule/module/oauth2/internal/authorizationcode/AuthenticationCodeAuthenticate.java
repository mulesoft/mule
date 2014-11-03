/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.oauth2.internal.authorizationcode.OAuthAuthenticationHeader.buildAuthorizationHeaderContent;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.HttpHeaders;
import org.mule.module.http.domain.HttpRequestBuilder;
import org.mule.module.http.request.HttpAuthentication;
import org.mule.module.oauth2.api.RequestAuthenticationException;
import org.mule.module.oauth2.internal.authorizationcode.state.UserOAuthContext;
import org.mule.util.AttributeEvaluator;

import org.apache.commons.lang.StringUtils;

public class AuthenticationCodeAuthenticate implements HttpAuthentication, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private AuthorizationCodeGrantType config;
    private AttributeEvaluator resourceOwnerIdEvaluator;

    @Override
    public void authenticate(MuleEvent muleEvent, HttpRequestBuilder builder) throws MuleException
    {
        final String resourceOwnerId = resourceOwnerIdEvaluator.resolveStringValue(muleEvent);
        if (resourceOwnerId == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("Evaluation of %s return an empty resourceOwnerId", resourceOwnerIdEvaluator.getRawValue())));
        }
        final String accessToken = config.getUserOAuthContext().getContextForUser(resourceOwnerId).getAccessToken();
        if (accessToken == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("No access token for the %s user. Verify that you have authenticated the user before trying to execute an operation to the API.", resourceOwnerId)));
        }
        builder.addHeader(HttpHeaders.Names.AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
    }

    @Override
    public boolean shouldRetry(final MuleEvent firstAttemptResponseEvent)
    {
        final String refreshTokenWhen = config.getRefreshTokenWhen();
        if (!StringUtils.isBlank(refreshTokenWhen))
        {
            final Object value = muleContext.getExpressionManager().evaluate(refreshTokenWhen, firstAttemptResponseEvent);
            if (!(value instanceof Boolean))
            {
                throw new MuleRuntimeException(createStaticMessage("Expression %s should return a boolean but return %s", config.getRefreshTokenWhen(), value));
            }
            final Boolean shouldRetryRequest = (Boolean) value;
            if (shouldRetryRequest)
            {
                try
                {
                    config.refreshToken(firstAttemptResponseEvent, resourceOwnerIdEvaluator.resolveStringValue(firstAttemptResponseEvent));
                }
                catch (MuleException e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
            return shouldRetryRequest;
        }
        return false;
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }

    public void setConfig(final AuthorizationCodeConfig config)
    {
        this.config = config;
    }

    public void setResourceOwnerId(final String resourceOwnerId)
    {
        resourceOwnerIdEvaluator = new AttributeEvaluator(resourceOwnerId);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (resourceOwnerIdEvaluator == null)
        {
            resourceOwnerIdEvaluator = new AttributeEvaluator(UserOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
        }
        resourceOwnerIdEvaluator.initialize(muleContext.getExpressionManager());
    }
}
