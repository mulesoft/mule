/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class WireTapCxfTestCase extends FunctionalTestCase
{
    static final Latch tapLatch = new Latch();
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        muleContext.registerListener(new FunctionalTestNotificationListener()
        {
            public void onNotification(ServerNotification notification)
            {
                tapLatch.release();
            }
        });
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/wire-tap-cxf.xml";
    }

    public void testWireTap() throws Exception
    {
        String url = "http://localhost:65082/services/EchoUMO";
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><echo><text>foo</text></echo></soap:Body></soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage response = client.send(url, msg, null);
        assertNotNull(response);
        
        String responseString = response.getPayloadAsString();
        assertTrue(responseString.contains("echoResponse"));
        assertFalse(responseString.contains("soap:Fault"));
                
        assertTrue(tapLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
