/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.events;

/**
 * <code>MuleSubscriptionEventListener</code> is a Spring ApplicationListener that
 * is used to register interest about Mule events. The developer can supply an array
 * of endpoints that it wishes to subscribe to. i.e. new String[]{
 * "file/C:/dev/test/data", "my.jms.queue", "http://www.mycompaony.com/events"}; You
 * can aslo specify logical endpoints that are configured on the Mule Server so you
 * can use more friendly names such as new String[]{ "testData", "OrdersJms",
 * "eventsHttp"}; By specifying '*' as the subscription, all events will be received
 * by this listener.
 */

public interface MuleSubscriptionEventListener extends MuleEventListener
{
    public String[] getSubscriptions();

    public void setSubscriptions(String[] subscriptions);
}
