/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.RegistryContext;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleEvent;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.model.AbstractComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultReplyToHandler</code> is responsible for processing a message
 * replyTo header.
 */

public class DefaultReplyToHandler implements ReplyToHandler
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DefaultReplyToHandler.class);

    private volatile UMOTransformer transformer;
    private final Map endpointCache = new HashMap();

    public DefaultReplyToHandler(UMOTransformer transformer)
    {
        this.transformer = transformer;
    }

    public void processReplyTo(UMOEvent event, UMOMessage returnMessage, Object replyTo) throws UMOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("sending reply to: " + returnMessage.getReplyTo());
        }

        String replyToEndpoint = replyTo.toString();

        // get the endpoint for this url
        UMOEndpoint endpoint = getEndpoint(event, replyToEndpoint);

        if (transformer == null)
        {
            transformer = event.getEndpoint().getResponseTransformer();
        }

        if (transformer != null)
        {
            endpoint.setTransformer(transformer);
        }

        // make sure remove the replyTo property as not cause a a forever
        // replyto loop
        returnMessage.removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);

        // Create the replyTo event asynchronous
        UMOEvent replyToEvent = new MuleEvent(returnMessage, endpoint, event.getSession(), false);

        // dispatch the event
        try
        {
            endpoint.dispatch(replyToEvent);
            if (logger.isInfoEnabled())
            {
                logger.info("reply to sent: " + endpoint);
            }
            ((AbstractComponent) event.getComponent()).getStatistics().incSentReplyToEvent();
        }
        catch (Exception e)
        {
            throw new DispatchException(
                CoreMessages.failedToDispatchToReplyto(endpoint),
                replyToEvent.getMessage(), replyToEvent.getEndpoint(), e);
        }

    }

    protected synchronized UMOEndpoint getEndpoint(UMOEvent event, String endpointUri) throws UMOException
    {
        UMOEndpoint endpoint = (UMOEndpoint)endpointCache.get(endpointUri);
        if (endpoint == null)
        {
            endpoint = RegistryContext.getRegistry().lookupEndpoint(endpointUri);
            if (endpoint != null)
            {
                // TODO MULE-2066: We should not need to set correct transformer
                // here, this should rather be done generically based on the endpoint
                // type.
                endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER);
                endpoint.setTransformer(((AbstractConnector) endpoint.getConnector()).getDefaultOutboundTransformer());
            }
            else
            {
                UMOEndpointURI ep = new MuleEndpointURI(endpointUri);
                endpoint = event.getManagementContext().getRegistry().getOrCreateEndpointForUri(ep,
                    UMOEndpoint.ENDPOINT_TYPE_SENDER);
                endpointCache.put(endpointUri, endpoint);
            }
        }
        return endpoint;
    }

    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    public void setTransformer(UMOTransformer transformer)
    {
        this.transformer = transformer;
    }

}
