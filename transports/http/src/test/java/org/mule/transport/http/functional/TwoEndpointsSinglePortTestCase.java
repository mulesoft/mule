/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transformer.DataType;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TwoEndpointsSinglePortTestCase extends FunctionalTestCase
{
    
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "two-endpoints-single-port.xml";
    }

    @Test
    public void testSendToEach() throws Exception
    {
        sendWithResponse("inMyComponent1", "test", "mycomponent1", 10);
        sendWithResponse("inMyComponent2", "test", "mycomponent2", 10);
    }

    @Test
    public void testSendToEachWithBadEndpoint() throws Exception
    {

        MuleClient client = new MuleClient(muleContext);

        sendWithResponse("inMyComponent1", "test", "mycomponent1", 5);
        sendWithResponse("inMyComponent2", "test", "mycomponent2", 5);

        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/mycomponent-notfound", "test", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        final int status = result.getInboundProperty("http.status", 0);
        assertEquals(404, status);

        // Test that after the exception the endpoints still receive events
        sendWithResponse("inMyComponent1", "test", "mycomponent1", 5);
        sendWithResponse("inMyComponent2", "test", "mycomponent2", 5);
    }

    protected void sendWithResponse(String endPointName, String message, String response, int noOfMessages)
        throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);

        List results = new ArrayList();
        for (int i = 0; i < noOfMessages; i++)
        {
            results.add(client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject(endPointName)).getAddress(), message, null).getPayload(DataType.BYTE_ARRAY_DATA_TYPE));
        }

        assertEquals(noOfMessages, results.size());
        for (int i = 0; i < noOfMessages; i++)
        {
            assertEquals(response, new String((byte[])results.get(i)));
        }
    }

}
