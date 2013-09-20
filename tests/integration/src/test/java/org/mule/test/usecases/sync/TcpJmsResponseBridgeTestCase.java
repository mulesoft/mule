/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class TcpJmsResponseBridgeTestCase extends FunctionalTestCase
{

    @Rule
    public final DynamicPort tcpPort = new DynamicPort("tcpPort");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/sync/tcp-jms-response-bridge.xml";
    }

    @Test
    public void testSyncResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = client.send("tcp://localhost:" + tcpPort.getNumber(), "request", null);
        assertNotNull(message);
        assertEquals("Received: request", message.getPayloadAsString());
    }
}
