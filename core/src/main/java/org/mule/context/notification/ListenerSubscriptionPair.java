/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
