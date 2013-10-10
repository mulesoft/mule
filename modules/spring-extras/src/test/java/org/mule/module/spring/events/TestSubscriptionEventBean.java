/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.events;

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
