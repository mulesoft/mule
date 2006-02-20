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
package org.mule.providers.gs;

import org.mule.MuleManager;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestNotification;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.umo.manager.UMOServerNotification;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GSFunctionalTestCase extends FunctionalTestCase implements FunctionalTestNotificationListener {

    private static int unprocessedCount = 0;
    private static int processedCount = 0;

    protected String checkPreReqs() {
        if(System.getProperty("com.gs.home", null) != null) {
            System.setProperty("com.gs.security.enabled", "false");
            System.setProperty("java.security.policy", System.getProperty("com.gs.home") + "/bin/policy.all");
            return null;
        }
        return "com.gs.home VM parameter not set.";
    }

    protected void doPostFunctionalSetUp() throws Exception {
        unprocessedCount = 0;
        processedCount = 0;
        MuleManager.getInstance().registerListener(this);
    }

    protected String getConfigResources() {
        return "mule-space-example.xml";

    }

    public void testReceivesWithTemplates() throws Exception {
        if(!isPrereqsMet("org.mule.providers.gs.GSFunctionalTestCase.testReceivesWithTemplates()")) return;

        MuleClient client = new MuleClient();
        Order order = new Order();
        order.setProcessed(Boolean.FALSE);
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(2000L);

        assertEquals(1, unprocessedCount);
        assertEquals(0, processedCount);
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(1000L);
        assertEquals(2, unprocessedCount);
        assertEquals(0, processedCount);

        order.setProcessed(Boolean.TRUE);
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(1000L);
        assertEquals(1, processedCount);
        assertEquals(2, unprocessedCount);
        client.send("gs:java://localhost/mule-space_container/mule-space?schema=cache", order, null);
        Thread.sleep(1000L);
        assertEquals(2, processedCount);
        assertEquals(2, unprocessedCount);

    }


    public void onNotification(UMOServerNotification notification) {
        if (notification.getAction() == FunctionalTestNotification.EVENT_RECEIVED) {
            if (notification.getResourceIdentifier().equals("unprocessedOrders")) {
                unprocessedCount++;
            } else if (notification.getResourceIdentifier().equals("processedOrders")) {
                processedCount++;
            }
        }
    }
}
