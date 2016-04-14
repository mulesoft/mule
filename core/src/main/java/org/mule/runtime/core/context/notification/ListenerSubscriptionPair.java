/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.util.ClassUtils;

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
        super();
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

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{listener, subscription, nullSubscription});
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        ListenerSubscriptionPair other = (ListenerSubscriptionPair) obj;
        return ClassUtils.equal(listener, other.listener) 
            && ClassUtils.equal(subscription, other.subscription)
            && (nullSubscription == other.nullSubscription);
    }

    @Override
    public String toString()
    {
        return "ListenerSubscriptionPair [listener=" + listener + ", subscription=" + subscription + "]";
    } 

}
