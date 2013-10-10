/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.routing.filters.WildcardFilter;

/**
 * This does the work necessary to deliver events to a particular listener.  It is generated for a
 * particular {@link Configuration} and stored in a
 * {@link org.mule.context.notification.Policy}.
 */
class Sender
{

    private ListenerSubscriptionPair pair;
    private WildcardFilter subscriptionFilter;

    Sender(ListenerSubscriptionPair pair)
    {
        this.pair = pair;
        subscriptionFilter = new WildcardFilter(pair.getSubscription());
        subscriptionFilter.setCaseSensitive(false);
    }

    public void dispatch(ServerNotification notification)
    {
        if (pair.isNullSubscription() ||
                (null != notification.getResourceIdentifier() &&
                        subscriptionFilter.accept(notification.getResourceIdentifier())))
        {
            try
            {
                pair.getListener().onNotification(notification);
            }
            catch (Exception e)
            {
                // Exceptions from listeners do not affect the notification processing
            }
        }
    }

}

