/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.NullSessionHandler;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.ModelNotification;
import org.mule.context.notification.MuleContextNotification;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <code>EndpointAbstractEventLoggerAgent</code> will forward server notifications
 * to a configurered endpoint uri.
 */
public class EndpointNotificationLoggerAgent extends AbstractNotificationLoggerAgent
{

    private OutboundEndpoint endpoint = null;
    private MuleSession session;
    private List ignoredNotifications = new ArrayList();


    public EndpointNotificationLoggerAgent()
    {
        super("Endpoint Logger Agent");
        // List of notifications to ignore, because when these notifications are
        // received the notification endpoint is no longer available
        ignoredNotifications.add(new Integer(MuleContextNotification.CONTEXT_STOPPED));
        ignoredNotifications.add(new Integer(MuleContextNotification.CONTEXT_DISPOSING));
        ignoredNotifications.add(new Integer(MuleContextNotification.CONTEXT_DISPOSED));
        ignoredNotifications.add(new Integer(ModelNotification.MODEL_STOPPED));
        ignoredNotifications.add(new Integer(ModelNotification.MODEL_DISPOSING));
        ignoredNotifications.add(new Integer(ModelNotification.MODEL_DISPOSED));
    }

    protected void doInitialise() throws InitialisationException
    {
        // first see if we're logging notifications to an endpoint
        try
        {
            if (endpoint == null)
            {
                throw new InitialisationException(CoreMessages.propertiesNotSet("endpoint"), this);
            }
            // Create a session for sending notifications
            session = new DefaultMuleSession(new DefaultMuleMessage(NullPayload.getInstance(), (Map) null), new NullSessionHandler(), muleContext);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected void logEvent(ServerNotification e)
    {
        if (endpoint != null && !ignoredNotifications.contains(new Integer(e.getAction())))
        {
            if (!endpoint.getConnector().isStarted())
            {
                logger.warn("Endpoint not started: " + endpoint.getEndpointURI() + ". Cannot dispatch notification: " + e);
                return;
            }
            if ((e.getAction() == ConnectionNotification.CONNECTION_FAILED || e.getAction() == ConnectionNotification.CONNECTION_DISCONNECTED)
                    && (e.getSource()).equals(endpoint.getConnector()))
            {
                // If this is a CONNECTION_FAILED or
                // CONNECTION_DISCONNECTED notification for the same connector that
                // is being used for notifications then ignore.
                return;
            }
            MuleMessage msg = new DefaultMuleMessage(e, (Map) null);
            try
            {
                //TODO: Filters should really be applied by the endpoint
                if (endpoint.getFilter() != null && !endpoint.getFilter().accept(msg))
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Message not accepted with filter: " + endpoint.getFilter());
                    }
                    return;
                }

                MuleEvent event = new DefaultMuleEvent(msg, endpoint, session, false);
                endpoint.dispatch(event);
            }
            catch (Exception e1)
            {
                // TODO MULE-863: If this is an error, do something better than this
                logger.error("Failed to dispatch event: " + e.toString() + " over endpoint: " + endpoint
                        + ". Error is: " + e1.getMessage(), e1);
            }
        }
    }

    /**
     * Should be a 1 line description of the agent
     */
    public String getDescription()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getName()).append(": ");
        if (endpoint != null)
        {
            buf.append("Forwarding notifications to: " + endpoint.getEndpointURI().getAddress());
        }
        return buf.toString();
    }

    public OutboundEndpoint getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }
}
