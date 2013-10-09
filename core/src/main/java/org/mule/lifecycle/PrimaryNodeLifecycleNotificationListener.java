/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
