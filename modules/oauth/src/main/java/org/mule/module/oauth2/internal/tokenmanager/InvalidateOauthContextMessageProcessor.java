/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.tokenmanager;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.util.AttributeEvaluator;

/**
 * Clears the oauth context for a token manager and a resource owner id.
 */
public class InvalidateOauthContextMessageProcessor implements MessageProcessor, Initialisable, MuleContextAware
{

    private TokenManagerConfig config;
    private AttributeEvaluator resourceOwnerIdEvaluator;
    private MuleContext muleContext;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        final String resourceOwnerId = resourceOwnerIdEvaluator.resolveStringValue(event);
        if (resourceOwnerId == null)
        {
            throw new MessagingException(CoreMessages.createStaticMessage("Resource owner id cannot be null"), event, this);
        }
        config.getConfigOAuthContext().clearContextForResourceOwner(resourceOwnerId);
        return event;
    }

    public void setConfig(TokenManagerConfig config)
    {
        this.config = config;
    }

    public void setResourceOwnerId(String resourceOwnerId)
    {
        resourceOwnerIdEvaluator = new AttributeEvaluator(resourceOwnerId);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (resourceOwnerIdEvaluator == null)
        {
            resourceOwnerIdEvaluator = new AttributeEvaluator(ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
        }
        resourceOwnerIdEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
