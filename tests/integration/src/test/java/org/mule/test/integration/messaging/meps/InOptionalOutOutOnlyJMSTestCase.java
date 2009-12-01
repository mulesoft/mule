/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.broker.BrokerService;

// START SNIPPET: full-class
public class InOptionalOutOutOnlyJMSTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    private BrokerService broker;

    @Override
    protected void suitePreSetUp() throws Exception
    {
        broker = new BrokerService();
        broker.addConnector("tcp://localhost:61616");
        broker.start();
    }
    
    @Override
    protected void suitePostTearDown() throws Exception
    {
        broker.stop();
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only_JMS.xml";
    }

    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNotNull(result);
        assertEquals(NullPayload.getInstance(), result.getPayload());

        Map props = new HashMap();
        props.put("foo", "bar");
        result = client.send("inboundEndpoint", "some data", props, 20000);
        
        // Give JMS some time to dispatch
        Thread.sleep(200);

        // No temporary queues should have been created, used, or be being waited on for a result 
        // See MULE-4617
        assertEquals(0, broker.getAdminView().getTemporaryQueues().length);

        assertNotNull(result);

        assertEquals("foo header received", result.getPayload());
    }
}
// END SNIPPET: full-class
