/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.RequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.context.notification.ConnectorMessageNotification;
import org.mule.context.notification.NotificationHelper;
import org.mule.context.notification.ServerNotificationManager;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides a reusable way for concrete {@link MessageProcessPhase}s to fire notifications.
 */
public abstract class NotificationFiringProcessingPhase<Template extends MessageProcessTemplate> implements MessageProcessPhase<Template>, Comparable<MessageProcessPhase>, MuleContextAware
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private ConcurrentHashMap<ServerNotificationManager, NotificationHelper> notificationHelpers = new ConcurrentHashMap<>();

    private MuleContext muleContext;

    protected void fireNotification(MuleEvent event, int action)
    {
        try
        {
            if (event == null || VoidMuleEvent.getInstance().equals(event))
            {
                // Null result only happens when there's a filter in the chain.
                // Unfortunately a filter causes the whole chain to return null
                // and there's no other way to retrieve the last event but using the RequestContext.
                // see https://www.mulesoft.org/jira/browse/MULE-8670
                event = RequestContext.getEvent();
                if (event == null || VoidMuleEvent.getInstance().equals(event))
                {
                    return;
                }
            }
            getNotificationHelper(muleContext.getNotificationManager()).fireNotification(
                    event,
                    event.getMessageSourceURI() != null ? event.getMessageSourceURI().toString() : null,
                    event.getFlowConstruct(),
                    action);
        }
        catch (Exception e)
        {
            logger.warn("Could not fire notification. Action: " + action, e);
        }
    }

    protected NotificationHelper getNotificationHelper(ServerNotificationManager serverNotificationManager)
    {
        NotificationHelper notificationHelper = notificationHelpers.get(serverNotificationManager);
        if (notificationHelper == null)
        {
            notificationHelper = new NotificationHelper(serverNotificationManager, ConnectorMessageNotification.class, false);
            notificationHelpers.putIfAbsent(serverNotificationManager, notificationHelper);
        }
        return notificationHelper;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;

    }
}
