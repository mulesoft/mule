/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.callback;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSaveAccessTokenCallback implements SaveAccessTokenCallback
{

    /**
     * Message Processor
     */
    private MessageProcessor messageProcessor;
    private static Logger logger = LoggerFactory.getLogger(DefaultSaveAccessTokenCallback.class);
    private boolean hasBeenStarted;
    private boolean hasBeenInitialized;

    public DefaultSaveAccessTokenCallback()
    {
        hasBeenStarted = false;
        hasBeenInitialized = false;
    }

    /**
     * Retrieves messageProcessor
     */
    public MessageProcessor getMessageProcessor()
    {
        return this.messageProcessor;
    }

    /**
     * Sets messageProcessor
     * 
     * @param value Value to set
     */
    public void setMessageProcessor(MessageProcessor value)
    {
        this.messageProcessor = value;
    }

    @Override
    public void saveAccessToken(String accessToken, String accessTokenSecret)
    {
        MuleEvent event = RequestContext.getEvent();
        if (accessToken != null)
        {
            event.getMessage().setInvocationProperty("OAuthAccessToken", accessToken);
        }
        if (accessTokenSecret != null)
        {
            event.getMessage().setInvocationProperty("OAuthAccessTokenSecret", accessTokenSecret);
        }
        if (messageProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) messageProcessor).setMuleContext(RequestContext.getEventContext()
                .getMuleContext());
        }
        if (messageProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) messageProcessor).setFlowConstruct(RequestContext.getEventContext()
                .getFlowConstruct());
        }
        if (!hasBeenInitialized)
        {
            if (messageProcessor instanceof Initialisable)
            {
                try
                {
                    ((Initialisable) messageProcessor).initialise();
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
            hasBeenInitialized = true;
        }
        if (!hasBeenStarted)
        {
            if (messageProcessor instanceof Startable)
            {
                try
                {
                    ((Startable) messageProcessor).start();
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
            hasBeenStarted = true;
        }
        try
        {
            messageProcessor.process(event);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }

}
