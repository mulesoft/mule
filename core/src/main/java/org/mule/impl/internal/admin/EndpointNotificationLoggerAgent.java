/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.admin;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.NullSessionHandler;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;

import java.util.Map;

/**
 * <code>EndpointAbstractEventLoggerAgent</code> will forward server notifications
 * to a configurered endpoint uri.
 */
public class EndpointNotificationLoggerAgent extends AbstractNotificationLoggerAgent
{

    private String endpointAddress;
    private UMOEndpoint logEndpoint = null;
    private UMOSession session;

    protected void doInitialise() throws InitialisationException
    {
        // first see if we're logging notifications to an endpoint
        try
        {
            if (endpointAddress != null)
            {
                logEndpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointAddress,
                    UMOEndpoint.ENDPOINT_TYPE_SENDER);
            }
            else
            {
                throw new InitialisationException(new Message(Messages.PROPERTIES_X_NOT_SET,
                    "endpointAddress"), this);
            }
            // Create a session for sending notifications
            session = new MuleSession(new MuleMessage(new NullPayload(), (Map)null), new NullSessionHandler());
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected void logEvent(UMOServerNotification e)
    {
        if (logEndpoint != null)
        {
            try
            {
                UMOMessage msg = new MuleMessage(e.toString(), (Map)null);
                UMOEvent event = new MuleEvent(msg, logEndpoint, session, false);
                logEndpoint.dispatch(event);
            }
            catch (Exception e1)
            {
                logger.error("Failed to dispatch event: " + e.toString() + " over endpoint: " + logEndpoint
                             + ". Error is: " + e1.getMessage(), e1);
            }
        }
    }

    /**
     * Should be a 1 line description of the agent
     * 
     * @return
     */
    public String getDescription()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getName()).append(": ");
        if (endpointAddress != null)
        {
            buf.append("Forwarding notifications to: " + endpointAddress);
        }
        return buf.toString();
    }

    public String getEndpointAddress()
    {
        return endpointAddress;
    }

    public void setEndpointAddress(String endpointAddress)
    {
        this.endpointAddress = endpointAddress;
    }
}
