/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.events;

/**
 * <code>MuleSubscriptionEventListener</code> is a Spring ApplicationListener
 * that is used to register interest about Mule events.
 * 
 * The developer can supply an array of endpoints that it wishes to subscribe
 * to. i.e. new String[]{ "file/C:/dev/test/data", "my.jms.queue",
 * "http://www.mycompaony.com/events"};
 * 
 * You can aslo specify logical endpoints that are configured on the Mule Server
 * so you can use more friendly names such as
 * 
 * new String[]{ "testData", "OrdersJms", "eventsHttp"};
 * 
 * By specifying '*' as the subscription, all events will be received by this
 * listener.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface MuleSubscriptionEventListener extends MuleEventListener
{
    public String[] getSubscriptions();

    public void setSubscriptions(String[] subscriptions);
}
