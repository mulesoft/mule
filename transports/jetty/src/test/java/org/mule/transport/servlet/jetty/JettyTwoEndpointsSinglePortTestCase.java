/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.DataType;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JettyTwoEndpointsSinglePortTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public JettyTwoEndpointsSinglePortTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jetty-two-endpoints-single-port-service.xml"},
            {ConfigVariant.FLOW, "jetty-two-endpoints-single-port-flow.xml"}
        });
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
        MuleClient client = muleContext.getClient();

        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent2", "test", "mycomponent2", 5);

        final HttpRequestOptions httpRequestOptions = newOptions().disableStatusCodeValidation().build();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/mycomponent-notfound", getTestMuleMessage(), httpRequestOptions);
        assertNotNull(result);
        final int status = result.getInboundProperty("http.status", 0);
        assertEquals(404, status);

        // Test that after the exception the endpoints still receive events
        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:" + dynamicPort.getNumber() + "/mycomponent2", "test", "mycomponent2", 5);
    }

    protected void sendWithResponse(String endpoint, String message, String response, int noOfMessages)
        throws MuleException
    {
        MuleClient client = muleContext.getClient();

        List<Object> results = new ArrayList<Object>();
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
