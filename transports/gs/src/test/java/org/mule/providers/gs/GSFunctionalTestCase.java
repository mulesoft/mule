/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import org.mule.MuleManager;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestNotification;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.umo.manager.UMOServerNotification;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class GSFunctionalTestCase extends FunctionalTestCase implements FunctionalTestNotificationListener
{
    private AtomicInteger unprocessedCount = new AtomicInteger(0);
    private AtomicInteger processedCount = new AtomicInteger(0);

    protected String checkPreReqs()
    {
        if (System.getProperty("com.gs.home", null) != null)
        {
            System.setProperty("com.gs.security.enabled", "false");
            System.setProperty("java.security.policy", System.getProperty("com.gs.home")
                                                       + "/policy/policy.all");
            return null;
        }
        return "com.gs.home VM parameter not set.";
    }

    protected void doPostFunctionalSetUp() throws Exception
    {
        unprocessedCount.set(0);
        processedCount.set(0);
        MuleManager.getInstance().registerListener(this);
    }

    protected String getConfigResources()
    {
        return "mule-space-example.xml";
    }

    public void testReceivesWithTemplates() throws Exception
    {
        if (!isPrereqsMet("org.mule.providers.gs.GSFunctionalTestCase.testReceivesWithTemplates()"))
        {
            return;
        }

        MuleClient client = new MuleClient();
        Order order = new Order();
        order.setProcessed(Boolean.FALSE);
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(2000L);

        assertEquals(1, unprocessedCount.get());
        assertEquals(0, processedCount.get());
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(1000L);
        assertEquals(2, unprocessedCount.get());
        assertEquals(0, processedCount.get());

        order.setProcessed(Boolean.TRUE);
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(1000L);
        assertEquals(1, processedCount.get());
        assertEquals(2, unprocessedCount.get());
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(1000L);
        assertEquals(2, processedCount.get());
        assertEquals(2, unprocessedCount.get());
    }

    public void onNotification(UMOServerNotification notification)
    {
        if (notification.getAction() == FunctionalTestNotification.EVENT_RECEIVED)
        {
            String resource = notification.getResourceIdentifier();
            if ("unprocessedOrders".equals(resource))
            {
                unprocessedCount.incrementAndGet();
            }
            else if ("processedOrders".equals(resource))
            {
                processedCount.incrementAndGet();
            }
        }
    }

}
