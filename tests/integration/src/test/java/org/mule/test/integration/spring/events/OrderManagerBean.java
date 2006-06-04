/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.spring.events;

import org.mule.extras.spring.events.MuleApplicationEvent;
import org.mule.extras.spring.events.MuleSubscriptionEventListener;
import org.springframework.context.ApplicationEvent;

/**
 * <code>OrderManagerBean</code> receives order beans from Mule and dispatches
 * process relsults back through Mule via the applicationContext
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class OrderManagerBean extends TestMuleEventBean implements OrderManager, MuleSubscriptionEventListener
{
    private String[] subscriptions;

    public void onApplicationEvent(ApplicationEvent event)
    {
        super.onApplicationEvent(event);
        // Get the order
        Order order = (Order) event.getSource();
        String result = processOrder(order);

        // Cast the event to a Mule event, we'll use this to get the AppContext
        MuleApplicationEvent muleEvent = (MuleApplicationEvent) event;

        // Create a new MuleEvent. This will be sent to the replyTo
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
