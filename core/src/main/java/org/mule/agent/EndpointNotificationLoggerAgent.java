/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.agent;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.ModelNotification;
import org.mule.context.notification.MuleContextNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>EndpointAbstractEventLoggerAgent</code> will forward server notifications
 * to a configurered endpoint uri.
 */
public class EndpointNotificationLoggerAgent extends AbstractNotificationLoggerAgent
{

    private OutboundEndpoint endpoint = null;
    private List<Integer> ignoredNotifications = new ArrayList<Integer>();


    public EndpointNotificationLoggerAgent()
    {
        super("Endpoint Logger Agent");
        // List of notifications to ignore, because when these notifications are
        // received the notification endpoint is no longer available
        ignoredNotifications.add(MuleContextNotification.CONTEXT_STOPPED);
        ignoredNotifications.add(MuleContextNotification.CONTEXT_DISPOSING);
        ignoredNotifications.add(MuleContextNotification.CONTEXT_DISPOSED);
        ignoredNotifications.add(ModelNotification.MODEL_STOPPED);
        ignoredNotifications.add(ModelNotification.MODEL_DISPOSED);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        // first see if we're logging notifications to an endpoint
        try
        {
            if (endpoint == null)
            {
                throw new InitialisationException(CoreMessages.propertiesNotSet("endpoint"), this);
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
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
            MuleMessage msg = new DefaultMuleMessage(e, muleContext);
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

                MuleEvent event = new DefaultMuleEvent(msg, endpoint.getExchangePattern(),
                    (FlowConstruct) null);
                event.setEnableNotifications(false);
                endpoint.process(event);
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
    @Override
    public String getDescription()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getName()).append(": ");
        if (endpoint != null)
        {
            buf.append("Forwarding notifications to: ").append(endpoint.getEndpointURI().getAddress());
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
