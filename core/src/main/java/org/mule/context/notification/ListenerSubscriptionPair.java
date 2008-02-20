/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotificationListener;

/**
 * A simple tuple that stores a listener with an optional subscription (used to match a resource ID).
 */
public class ListenerSubscriptionPair
{

    private ServerNotificationListener listener;
    private String subscription = ServerNotificationManager.NULL_SUBSCRIPTION;
    private boolean nullSubscription = true;

    /**
     * For config - must be constructed using the setters
     */
    public ListenerSubscriptionPair()
    {
        // empty
    }

    public ListenerSubscriptionPair(ServerNotificationListener listener)
    {
        setListener(listener);
    }

    public ListenerSubscriptionPair(ServerNotificationListener listener, String subscription)
    {
        setListener(listener);
        setSubscription(subscription);
    }

    public void setListener(ServerNotificationListener listener)
    {
        this.listener = listener;
    }

    public void setSubscription(String subscription)
    {
        if (null != subscription)
        {
            this.subscription = subscription;
            nullSubscription = false;
        }
    }

    public ServerNotificationListener getListener()
    {
        return listener;
    }

    public String getSubscription()
    {
        return subscription;
    }

    public boolean isNullSubscription()
    {
        return nullSubscription;
    }

}
