/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

/**
 * <code>TestSubscriptionEventBean</code> TODO
 */

public class TestSubscriptionEventBean extends TestMuleEventBean implements MuleSubscriptionEventListener
{
    private String[] subscriptions;

    public void setSubscriptions(String[] subscriptions)
    {
        this.subscriptions = subscriptions;
    }

    public String[] getSubscriptions()
    {
        return subscriptions;
    }

}
