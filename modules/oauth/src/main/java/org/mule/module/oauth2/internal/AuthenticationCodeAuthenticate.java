/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.HttpHeaders;
import org.mule.module.http.request.HttpAuth;
import org.mule.module.oauth2.api.RequestAuthenticationException;
import org.mule.module.oauth2.internal.state.UserOAuthState;
import org.mule.util.AttributeEvaluator;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.api.Authentication;

public class AuthenticationCodeAuthenticate implements HttpAuth, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private AuthorizationCodeGrantType config;
    private AttributeEvaluator oauthStateIdEvaluator;

    @Override
    public void authenticate(final MuleEvent muleEvent) throws MuleException
    {
        final String oauthStateId = oauthStateIdEvaluator.resolveStringValue(muleEvent);
        if (oauthStateId == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("Evaluation of %s return an empty oauthStateId", oauthStateIdEvaluator.getRawValue())));
        }
        final String accessToken = config.getOAuthState().getStateForUser(oauthStateId).getAccessToken();
        if (accessToken == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("No access token for the %s user. Verify that you have authenticated the user before trying to execute an operation to the API.", oauthStateId)));
        }
        muleEvent.getMessage().setOutboundProperty(HttpHeaders.Names.AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
    }

    public static String buildAuthorizationHeaderContent(String accessToken)
    {
        return "Bearer " + accessToken;
    }

    @Override
    public Authentication buildAuthentication()
    {
        //TODO remove once we fix the HttpAuth interface in the new http module.
        return null;
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
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("Expression %s should return a boolean but return %s", config.getRefreshTokenWhen(), value));
            }
            final Boolean shouldRetryRequest = (Boolean) value;
            if (shouldRetryRequest)
            {
                try
                {
                    config.refreshToken(firstAttemptResponseEvent, oauthStateIdEvaluator.resolveStringValue(firstAttemptResponseEvent));
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

    public void setOauthStateId(final String oauthStateId)
    {
        oauthStateIdEvaluator = new AttributeEvaluator(oauthStateId);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (oauthStateIdEvaluator == null)
        {
            oauthStateIdEvaluator = new AttributeEvaluator(UserOAuthState.DEFAULT_USER_ID);
        }
        oauthStateIdEvaluator.initialize(muleContext.getExpressionManager());
    }
}
