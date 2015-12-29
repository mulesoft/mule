/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.broker.BrokerService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-6926: Flaky test")
public class InOptionalOutOutOnlyJMSTestCase extends FunctionalTestCase
{
    @ClassRule
    public static DynamicPort serverPort = new DynamicPort("serverPort");

    public static final long TIMEOUT = 3000;

    private static BrokerService broker;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only_JMS-flow.xml";
    }

    @BeforeClass
    public static void startBroker() throws Exception
    {
        broker = new BrokerService();
        broker.addConnector("tcp://localhost:" + serverPort.getNumber());
        broker.start();
    }

    @AfterClass
    public static void stopBroker() throws Exception
    {
        broker.stop();
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNotNull(result);
        assertEquals(NullPayload.getInstance(), result.getPayload());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "bar");
        result = client.send("inboundEndpoint", "some data", props, 20000);

        // Give JMS some time to dispatch
        Thread.sleep(200);

        // No temporary queues should have been created, used, or be being waited on
        // for a result
        // See MULE-4617
        assertEquals(0, broker.getAdminView().getTemporaryQueues().length);

        assertNotNull(result);

        assertEquals("foo header received", result.getPayload());
    }
}
