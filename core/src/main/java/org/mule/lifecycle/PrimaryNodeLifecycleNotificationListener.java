/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ClusterNodeNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.LifecycleStateEnabled;
import org.mule.api.lifecycle.Startable;
import org.mule.context.notification.NotificationException;

/**
 *
 * This class will start an Startable mule object that must only be started in the primary node.
 *
 */
public class PrimaryNodeLifecycleNotificationListener implements ClusterNodeNotificationListener {

    protected transient Log logger = LogFactory.getLog(getClass());
    private Startable startMeOnPrimaryNodeNotification;
    private MuleContext muleContext;

    public PrimaryNodeLifecycleNotificationListener(Startable startMeOnPrimaryNodeNotification, MuleContext muleContext) {
        this.startMeOnPrimaryNodeNotification = startMeOnPrimaryNodeNotification;
        this.muleContext = muleContext;
    }

    public void register()
    {
        try
        {
            if (muleContext != null)
            {
                muleContext.registerListener(this);
            }
        }
        catch (NotificationException e)
        {
            throw new RuntimeException("Unable to register listener", e);
        }
    }

    @Override
    public void onNotification(ServerNotification notification)
    {
        try
        {
            if (startMeOnPrimaryNodeNotification instanceof LifecycleState)
            {
                if (((LifecycleState)startMeOnPrimaryNodeNotification).isStarted())
                {
                    startMeOnPrimaryNodeNotification.start();
                }
                else
                {
                    logStartableNotStartedMessage();
                }
            }
            else if (startMeOnPrimaryNodeNotification instanceof LifecycleStateEnabled)
            {
                if (((LifecycleStateEnabled)startMeOnPrimaryNodeNotification).getLifecycleState().isStarted())
                {
                    startMeOnPrimaryNodeNotification.start();
                }
                else
                {
                    logStartableNotStartedMessage();
                }
            }
            else 
            {
                startMeOnPrimaryNodeNotification.start();
            }
        }
        catch (MuleException e)
        {
            throw new RuntimeException("Error starting wrapped message source", e);
        }
    }

    private void logStartableNotStartedMessage()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Not starting Startable since it's not in started state");
        }
    }

    public void unregister()
    {
        muleContext.unregisterListener(this);
    }
}
