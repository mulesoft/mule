/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


