/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.module.oauth2.api.RequestAuthenticationException;
import org.mule.module.oauth2.internal.AbstractGrantType;
import org.mule.module.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.util.AttributeEvaluator;

import org.apache.commons.lang.StringUtils;

/**
 * Represents the config element for oauth:authentication-code-config.
 * <p/>
 * This config will:
 * - If the authorization-request is defined then it will create a flow listening for an user call to begin the oauth login.
 * - If the token-request is defined then it will create a flow for listening in the redirect uri so we can get the authentication code and retrieve the access token
 */
public class DefaultAuthorizationCodeGrantType extends AbstractGrantType implements Initialisable, AuthorizationCodeGrantType, Startable, Stoppable, MuleContextAware
{

    public static final String EXTERNAL_REDIRECT_URL_PROPERTY = SYSTEM_PROPERTY_PREFIX + "oauth2.externalRedirectUrl";

    private String redirectionUrl;
    private String externalRedirectionUrl;
    private AuthorizationRequestHandler authorizationRequestHandler;
    private AbstractAuthorizationCodeTokenRequestHandler tokenRequestHandler;
    private MuleContext muleContext;
    private AttributeEvaluator localAuthorizationUrlResourceOwnerIdEvaluator;
    private AttributeEvaluator resourceOwnerIdEvaluator;

    public void setRedirectionUrl(final String redirectionUrl)
    {
        this.redirectionUrl = redirectionUrl;
    }

    public void setExternalRedirectionUrl(final String externalRedirectionUrl)
    {
        this.externalRedirectionUrl = externalRedirectionUrl;
    }

    public void setAuthorizationRequestHandler(final AuthorizationRequestHandler authorizationRequestHandler)
    {
        this.authorizationRequestHandler = authorizationRequestHandler;
    }

    public void setTokenRequestHandler(final AbstractAuthorizationCodeTokenRequestHandler tokenRequestHandler)
    {
        this.tokenRequestHandler = tokenRequestHandler;
    }

    public ConfigOAuthContext getConfigOAuthContext()
    {
        return tokenManagerConfig.getConfigOAuthContext();
    }

    public String getRedirectionUrl()
    {
        return redirectionUrl;
    }

    public String getExternalRedirectionUrl()
    {
        return externalRedirectionUrl;
    }

    @Override
    public String getRefreshTokenWhen()
    {
        return tokenRequestHandler.getRefreshTokenWhen();
    }

    public AttributeEvaluator getLocalAuthorizationUrlResourceOwnerIdEvaluator()
    {
        return localAuthorizationUrlResourceOwnerIdEvaluator;
    }

    public AttributeEvaluator getResourceOwnerIdEvaluator()
    {
        return resourceOwnerIdEvaluator;
    }

    @Override
    public void refreshToken(final MuleEvent currentFlowEvent, final String resourceOwnerId) throws MuleException
    {
        tokenRequestHandler.refreshToken(currentFlowEvent, resourceOwnerId);
    }

    @Override
    public ConfigOAuthContext getUserOAuthContext()
    {
        return tokenManagerConfig.getConfigOAuthContext();
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            if (tokenManagerConfig == null)
            {
                this.tokenManagerConfig = TokenManagerConfig.createDefault(muleContext);
                this.tokenManagerConfig.initialise();
            }
            if (localAuthorizationUrlResourceOwnerIdEvaluator == null)
            {
                localAuthorizationUrlResourceOwnerIdEvaluator = new AttributeEvaluator(null);
            }
            localAuthorizationUrlResourceOwnerIdEvaluator.initialize(muleContext.getExpressionManager());
            if (resourceOwnerIdEvaluator == null)
            {
                resourceOwnerIdEvaluator = new AttributeEvaluator(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
            }
            resourceOwnerIdEvaluator.initialize(muleContext.getExpressionManager());

            String externalRedirectionUrlSystemProperty = System.getProperty(EXTERNAL_REDIRECT_URL_PROPERTY);
            if (externalRedirectionUrl != null && externalRedirectionUrlSystemProperty != null)
            {
                throw new IllegalArgumentException("Can't use both externalRedirectionUrl and " + EXTERNAL_REDIRECT_URL_PROPERTY);
            }
            if (externalRedirectionUrl == null)
            {
                externalRedirectionUrl = externalRedirectionUrlSystemProperty != null ? externalRedirectionUrlSystemProperty : getRedirectionUrl();
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public void authenticate(MuleEvent muleEvent, HttpRequestBuilder builder) throws MuleException
    {
        final String resourceOwnerId = resourceOwnerIdEvaluator.resolveStringValue(muleEvent);
        if (resourceOwnerId == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("Evaluation of %s return an empty resourceOwnerId", localAuthorizationUrlResourceOwnerIdEvaluator.getRawValue())));
        }
        final String accessToken = getAccessToken(getUserOAuthContext(), resourceOwnerId);       
        if (accessToken == null)
        {
            throw new RequestAuthenticationException(createStaticMessage(String.format("No access token for the %s user. Verify that you have authenticated the user before trying to execute an operation to the API.", resourceOwnerId)));
        }
        builder.addHeader(HttpHeaders.Names.AUTHORIZATION, buildAuthorizationHeaderContent(accessToken));
    }

    @Override
    public boolean shouldRetry(final MuleEvent firstAttemptResponseEvent) throws MuleException
    {
        if (!StringUtils.isBlank(getRefreshTokenWhen()))
        {
            final Object value = muleContext.getExpressionManager().evaluate(getRefreshTokenWhen(), firstAttemptResponseEvent);
            if (!(value instanceof Boolean))
            {
                throw new MuleRuntimeException(createStaticMessage("Expression %s should return a boolean but return %s", getRefreshTokenWhen(), value));
            }
            Boolean shouldRetryRequest = (Boolean) value;
            if (shouldRetryRequest)
            {
                try
                {
                    refreshToken(firstAttemptResponseEvent, resourceOwnerIdEvaluator.resolveStringValue(firstAttemptResponseEvent));
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

    public void setLocalAuthorizationUrlResourceOwnerId(final String resourceOwnerId)
    {
        localAuthorizationUrlResourceOwnerIdEvaluator = new AttributeEvaluator(resourceOwnerId);
    }

    public void setResourceOwnerId(String resourceOwnerId)
    {
        this.resourceOwnerIdEvaluator = new AttributeEvaluator(resourceOwnerId);
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
            tokenRequestHandler.setOauthConfig(this, proxyConfig);
            tokenRequestHandler.init();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        tokenRequestHandler.dispose();
    }
}
