/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.acegi;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.context.notification.SecurityNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class SecureHttpPollingFunctionalTestCase extends FunctionalTestCase
{

    public void testPollingHttpConnectorSentCredentials() throws Exception
    {
        final Latch latch = new Latch();
        muleContext.registerListener(new SecurityNotificationListener<SecurityNotification>()
        {
            public void onNotification(SecurityNotification notification)
            {
                latch.countDown();
            }
        });
        MuleClient client = new MuleClient();
        MuleMessage result = client.request("vm://toclient", 5000);
        assertNotNull(result);
        assertEquals("foo", result.getPayloadAsString());

        result = client.request("vm://toclient2", 1000);
        //This seems a little odd that we forward the exception to the outbound endpoint, but I guess users
        // can just add a filter
        assertNotNull(result);
        assertEquals(401, result.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));

    }

    protected String getConfigResources()
    {
        return "secure-http-polling-server.xml,secure-http-polling-client.xml";
    }
    
}
