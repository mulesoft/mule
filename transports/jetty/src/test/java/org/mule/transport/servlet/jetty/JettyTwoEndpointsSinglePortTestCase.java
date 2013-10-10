/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.module.client.MuleClient;
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
