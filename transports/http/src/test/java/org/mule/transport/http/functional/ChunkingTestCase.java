/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChunkingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "chunking-test.xml";
    }

    @Test
    public void testPartiallyReadRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        byte[] msg = new byte[100*1024];
        
        MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inMain")).getAddress(), 
            msg, null);
        assertEquals("Hello", result.getPayloadAsString());
        int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(200, status);
        
        result = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inMain")).getAddress(),
            msg, null);
        assertEquals("Hello", result.getPayloadAsString());
        status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(200, status);
    }

}


