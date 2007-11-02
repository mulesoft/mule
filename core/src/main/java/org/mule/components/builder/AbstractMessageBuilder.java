/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.builder;

import org.mule.config.ConfigurationException;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.impl.UMOComponentAware;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A component that will invoke all outbound endpoints configured on the component
 * allow the result of each endpoint invocation to be aggregated to a single message.
 */
public abstract class AbstractMessageBuilder implements UMOComponentAware, Callable, MessageBuilder
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected UMOComponent component;

    public void setComponent(UMOComponent component) throws ConfigurationException
    {
        this.component = component;
    }

    public Object onCall(UMOEventContext eventContext) throws Exception
    {

        UMOMessage requestMessage = new MuleMessage(eventContext.getTransformedMessage(),
            eventContext.getMessage());

        UMOMessage responseMessage = null;
        Object builtMessage;

        if (component.getOutboundRouter().hasEndpoints())
        {
            List endpoints = new ArrayList();
            for (Iterator iterator = component.getOutboundRouter().getRouters().iterator(); iterator.hasNext();)
            {
                UMOOutboundRouter router = (UMOOutboundRouter) iterator.next();
                endpoints.addAll(router.getEndpoints());
            }

            for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
            {
                UMOImmutableEndpoint endpoint = (UMOImmutableEndpoint) iterator.next();
                Object request = requestMessage.getPayload();

                boolean rsync = eventContext.getMessage().getBooleanProperty(
                    MuleProperties.MULE_REMOTE_SYNC_PROPERTY, endpoint.isRemoteSync());
                if (!rsync)
                {
                    logger.info("Endpoint: " + endpoint
                                + " is not remoteSync enabled. Message builder finishing");
                    if (eventContext.isSynchronous())
                    {
                        responseMessage = eventContext.sendEvent(requestMessage, endpoint);
                    }
                    else
                    {
                        eventContext.dispatchEvent(requestMessage, endpoint);
                        responseMessage = null;
                    }
                    break;
                }
                else
                {
                    responseMessage = eventContext.sendEvent(requestMessage, endpoint);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Response Message Received from: " + endpoint.getEndpointURI());
                    }
                    if (logger.isTraceEnabled())
                    {
                        try
                        {
                            logger.trace("Message Payload: \n"
                                         + StringMessageUtils.truncate(
                                             StringMessageUtils.toString(responseMessage.getPayload()), 200,
                                             false));
                        }
                        catch (Exception e)
                        {
                            // ignore
                        }
                    }
                    builtMessage = buildMessage(new MuleMessage(request, requestMessage), responseMessage);
                    responseMessage = new MuleMessage(builtMessage, responseMessage);
                    requestMessage = new MuleMessage(responseMessage.getPayload(), responseMessage);
                }
            }
        }
        else
        {
            logger.info("There are currently no endpoints configured on component: " + component.getName());
        }
        eventContext.setStopFurtherProcessing(true);
        return responseMessage;
    }
}
