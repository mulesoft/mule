/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.mule.MuleManager;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.util.concurrent.Latch;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class WireTapTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/wire-tap.xml";
    }

    public void testWireTap() throws Exception
    {
        final Latch receiverLatch = new Latch();
        final Latch tappedReceiverLatch = new Latch();
        MuleManager.getInstance().registerListener(new FunctionalTestNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
            {
                if (notification.getResourceIdentifier().equals("Receiver"))
                {
                    receiverLatch.countDown();
                }
                else if (notification.getResourceIdentifier().equals("TappedReceiver"))
                {
                    tappedReceiverLatch.countDown();
                }
            }
        });
        MuleClient client = new MuleClient();
        client.send("vm://inbound.channel", "test", null);
        assertTrue(receiverLatch.await(3L, TimeUnit.SECONDS));
        assertTrue(tappedReceiverLatch.await(1L, TimeUnit.SECONDS));
    }
}
