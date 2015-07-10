/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;

import java.io.NotSerializableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Until successful synchronous processing strategy.
 * It will execute the until-successful router within the callers thread.
 */
public class SynchronousUntilSuccessfulProcessingStrategy extends AbstractUntilSuccessfulProcessingStrategy implements Initialisable
{

    protected transient Log logger = LogFactory.getLog(getClass());

    @Override
    protected MuleEvent doRoute(MuleEvent event) throws MessagingException
    {
        Exception lastExecutionException = null;
        try
        {
            MuleEvent retryEvent = DefaultMuleEvent.copy(event);
            for (int i = 0; i <= getUntilSuccessfulConfiguration().getMaxRetries(); i++)
            {
                try
                {
                    return processResponseThroughAckResponseExpression(processEvent(event));
                }
                catch (Exception e)
                {
                    logger.info("Exception thrown inside until-successful " + e.getMessage());
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(e);
                    }
                    lastExecutionException = e;
                    if (i < getUntilSuccessfulConfiguration().getMaxRetries())
                    {
                        Thread.sleep(getUntilSuccessfulConfiguration().getMillisBetweenRetries());
                        event = DefaultMuleEvent.copy(retryEvent);
                    }
                }
            }
            throw new RoutingException(event, getUntilSuccessfulConfiguration().getRouter(), lastExecutionException);
        }
        catch (MessagingException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RoutingException(event, getUntilSuccessfulConfiguration().getRouter(), e);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (getUntilSuccessfulConfiguration().getThreadingProfile() != null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("Until successful cannot be configured to be synchronous and have a threading profile at the same time"), this);
        }
        if (getUntilSuccessfulConfiguration().getObjectStore() != null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("Until successful cannot be configured to be synchronous and use an object store."), this);
        }
        if (getUntilSuccessfulConfiguration().getDlqMP() != null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("Until successful cannot be configured to be synchronous and use a dead letter queue. Failure must be processed with exception strategy"), this);
        }
    }

    @Override
    protected void ensureSerializable(MuleMessage message) throws NotSerializableException
    {
        // Message is not required to be Serializable because it is kept in memory
    }

}
