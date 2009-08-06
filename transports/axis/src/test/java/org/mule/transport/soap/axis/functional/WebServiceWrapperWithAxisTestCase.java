/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis.functional;


import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Properties;

public class WebServiceWrapperWithAxisTestCase extends FunctionalTestCase
{
    private String testString = "test";
    
    public void testWsCall() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://testin", new DefaultMuleMessage(testString, muleContext));
        assertNotNull(result.getPayload());
        assertEquals("Payload", "Received: "+ testString, result.getPayloadAsString());
    }
    
    public void testWsCallWithUrlFromMessage() throws Exception
    {
        MuleClient client = new MuleClient();
        Properties props = new Properties();
        props.setProperty("ws.service.url", "http://localhost:65081/services/TestUMO?method=receive");
        MuleMessage result = client.send("vm://testin2", testString, props);
        assertNotNull(result.getPayload());
        assertEquals("Payload", "Received: "+ testString, result.getPayloadAsString());
    }
    
    public void testWsCallWithComplexParameters() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://queue.in", new Object[]{new Long(3), new Long(3)},null);
        MuleMessage result = client.request("vm://queue.out", 500000);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload() instanceof Long);
        assertEquals("Payload", 6, ((Long)result.getPayload()).intValue());
    }
    
    protected String getConfigResources()
    {
        return "mule-ws-wrapper-config.xml";
    }
}
