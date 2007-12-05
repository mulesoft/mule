/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications.manager;

import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.manager.UMOServerNotification;

/**
 * This does the work necessary to deliver events to a particular listener.  It is generated for a
 * particular {@link Configuration} and stored in a
 * {@link org.mule.impl.internal.notifications.manager.Policy}.
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

    public void dispatch(UMOServerNotification notification)
    {
        if (pair.isNullSubscription() ||
                (null != notification.getResourceIdentifier() &&
                        subscriptionFilter.accept(notification.getResourceIdentifier())))
        {
            pair.getListener().onNotification(notification);
        }
    }

}

