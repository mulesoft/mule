/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.oauth2.internal.authorizationcode.state.UserOAuthContext;
import org.mule.util.AttributeEvaluator;

/**
 * Stores oauth context for a particular resource owner id.
 */
public class StoreAuthenticationCodeStateMessageProcessor implements MessageProcessor, Initialisable, MuleContextAware
{

    private AuthorizationCodeGrantType config;
    private AttributeEvaluator resourceOwnerIdEvaluator;
    private MuleContext muleContext;
    private AttributeEvaluator accessTokenEvaluator;
    private AttributeEvaluator refreshTokenEvaluator;
    private AttributeEvaluator expiresInEvaluator;

    public void setConfig(final AuthorizationCodeGrantType config)
    {
        this.config = config;
    }

    /**
     * @param resourceOwnerId static value or expression that resolved an oauth state id
     */
    public void setResourceOwnerId(final String resourceOwnerId)
    {
        this.resourceOwnerIdEvaluator = new AttributeEvaluator(resourceOwnerId);
    }

    /**
     * @param accessToken a valid access token to use for authentication
     */
    public void setAccessToken(final String accessToken)
    {
        this.accessTokenEvaluator = new AttributeEvaluator(accessToken);
    }

    /**
     * @param refreshToken a valid refresh token to use for retrieving a new access token
     */
    public void setRefreshToken(final String refreshToken)
    {
        this.refreshTokenEvaluator = new AttributeEvaluator(refreshToken);
    }

    /**
     * @param expiresIn time expiration for the access token
     */
    public void setExpiresIn(final String expiresIn)
    {
        this.expiresInEvaluator = new AttributeEvaluator(expiresIn);
    }

    @Override
    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        final String resourceOwnerId = resourceOwnerIdEvaluator.resolveStringValue(event);
        if (resourceOwnerId == null)
        {
            throw new DefaultMuleException(CoreMessages.createStaticMessage(String.format("resourceOwnerId cannot be null. Following expression return null %s", resourceOwnerIdEvaluator.getRawValue())));
        }
        final String accessToken = accessTokenEvaluator.resolveStringValue(event);
        if (accessToken == null)
        {
            throw new DefaultMuleException(CoreMessages.createStaticMessage(String.format("accessToken cannot be null. Following expression return null %s", accessTokenEvaluator.getRawValue())));
        }
        final UserOAuthContext userState = config.getUserOAuthContext().getContextForUser(resourceOwnerId);
        userState.setAccessToken(accessToken);
        if (refreshTokenEvaluator != null)
        {
            userState.setRefreshToken(refreshTokenEvaluator.resolveStringValue(event));
        }
        if (expiresInEvaluator != null)
        {
            userState.setExpiresIn(expiresInEvaluator.resolveStringValue(event));
        }
        return event;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (config == null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("OAuth config must be configured"), this);
        }
        if (resourceOwnerIdEvaluator == null)
        {
            resourceOwnerIdEvaluator = new AttributeEvaluator(UserOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
        }
        if (accessTokenEvaluator == null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("accessToken must be configured"), this);
        }
        initialiseAttributeEvaluator(resourceOwnerIdEvaluator);
        initialiseAttributeEvaluator(accessTokenEvaluator);
        initialiseAttributeEvaluator(refreshTokenEvaluator);
        initialiseAttributeEvaluator(expiresInEvaluator);
    }

    private void initialiseAttributeEvaluator(final AttributeEvaluator attributeEvaluator)
    {
        if (attributeEvaluator != null)
        {
            attributeEvaluator.initialize(muleContext.getExpressionManager());
        }
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }
}
