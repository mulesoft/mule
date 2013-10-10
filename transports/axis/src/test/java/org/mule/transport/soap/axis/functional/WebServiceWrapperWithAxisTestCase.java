/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WebServiceWrapperWithAxisTestCase extends FunctionalTestCase
{
    private String testString = "test";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigResources()
    {
        return "mule-ws-wrapper-config.xml";
    }

    @Test
    public void testWsCall() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("vm://testin", new DefaultMuleMessage(testString, muleContext));
        assertNotNull(result.getPayload());
        assertEquals("Payload", "Received: " + testString, result.getPayloadAsString());
    }

    @Test
    public void testWsCallWithUrlFromMessage() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Properties props = new Properties();
        props.setProperty("ws.service.url", "http://localhost:" + dynamicPort1.getNumber() + "/services/TestUMO?method=receive");
        MuleMessage result = client.send("vm://testin2", testString, props);
        assertNotNull(result.getPayload());
        assertEquals("Payload", "Received: "+ testString, result.getPayloadAsString());
    }

    @Test
    public void testWsCallWithComplexParameters() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://queue.in", new Object[]{new Long(3), new Long(3)},null);
        MuleMessage result = client.request("vm://queue.out", RECEIVE_TIMEOUT);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload() instanceof Long);
        assertEquals("Payload", 6, ((Long)result.getPayload()).intValue());
    }
}
