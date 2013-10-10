/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
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

public class JettyTwoEndpointsSinglePortTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "jetty-two-endpoints-single-port.xml";
    }

    @Test
    public void testSendToEach() throws Exception
    {

        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent1", "test", "mycomponent1", 10);
        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent2", "test", "mycomponent2", 10);
    }

    @Test
    public void testSendToEachWithBadEndpoint() throws Exception
    {

        MuleClient client = new MuleClient(muleContext);

        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent2", "test", "mycomponent2", 5);

        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/mycomponent-notfound", "test", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        final int status = result.getInboundProperty("http.status", 0);
        assertEquals(404, status);

        // Test that after the exception the endpoints still receive events
        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent2", "test", "mycomponent2", 5);
    }

    protected void sendWithResponse(String endpoint, String message, String response, int noOfMessages)
        throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);

        List results = new ArrayList();
        for (int i = 0; i < noOfMessages; i++)
        {
            MuleMessage result = client.send(endpoint, message, null);
            results.add(result.getPayload(DataType.BYTE_ARRAY_DATA_TYPE));
        }

        assertEquals(noOfMessages, results.size());
        for (int i = 0; i < noOfMessages; i++)
        {
            assertEquals(response, new String((byte[])results.get(i)));
        }
    }
}
