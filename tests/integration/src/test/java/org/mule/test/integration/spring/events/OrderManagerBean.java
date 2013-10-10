/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.spring.events;

import org.mule.module.spring.events.MuleApplicationEvent;
import org.mule.module.spring.events.MuleSubscriptionEventListener;

import org.springframework.context.ApplicationEvent;

/**
 * <code>OrderManagerBean</code> receives Order beans from Mule and dispatches
 * processed results back through Mule via the applicationContext
 */
public class OrderManagerBean extends TestMuleEventBean
    implements OrderManager, MuleSubscriptionEventListener
{
    private String[] subscriptions;

    public void onApplicationEvent(ApplicationEvent event)
    {
        super.onApplicationEvent(event);
        // Get the order
        Order order = (Order)event.getSource();
        String result = processOrder(order);

        // Cast the event to a Mule event, we'll use this to get the AppContext
        MuleApplicationEvent muleEvent = (MuleApplicationEvent)event;

        // Create a new DefaultMuleEvent. This will be sent to the replyTo
        // address
        MuleApplicationEvent returnEvent = null;

        returnEvent = new MuleApplicationEvent(result, "jms://processed.queue");

        // Call publish on the application context, Mule will do the rest
        muleEvent.getApplicationContext().publishEvent(returnEvent);
    }

    public String processOrder(Order order)
    {
        // Do some processing...
        return "Order '" + order.getOrder() + "' Processed";
    }

    public String[] getSubscriptions()
    {
        return subscriptions;
    }

    public void setSubscriptions(String[] subscriptions)
    {
        this.subscriptions = subscriptions;
    }

}
