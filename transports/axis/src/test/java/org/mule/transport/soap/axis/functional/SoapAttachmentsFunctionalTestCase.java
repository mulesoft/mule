/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.message.ds.StringDataSource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.soap.axis.AxisMessageDispatcher;

import javax.activation.DataHandler;

import org.junit.Rule;
import org.junit.Test;

public class SoapAttachmentsFunctionalTestCase extends FunctionalTestCase
{
    private static final int SEND_COUNT = 5;
    
    private int callbackCount = 0;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "axis-soap-attachments.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        sendTestData(SEND_COUNT);
        assertEquals(SEND_COUNT, callbackCount);
    }

    protected void sendTestData(int iterations) throws Exception
    {
        OutboundEndpoint ep = muleContext.getRegistry().lookupEndpointBuilder("client").buildOutboundEndpoint();

        AxisMessageDispatcher client = new AxisMessageDispatcher(ep);
        client.initialise();
        for (int i = 0; i < iterations; i++)
        {
            MuleMessage msg = new DefaultMuleMessage("testPayload", muleContext);
            msg.addOutboundAttachment("testAttachment", new DataHandler(new StringDataSource("foo")));
            DefaultMuleEvent event = new DefaultMuleEvent(msg, getTestInboundEndpoint("test://test"),
                (FlowConstruct) null);
            MuleMessage result = client.process(event).getMessage();
            assertNotNull(result);
            assertNotNull(result.getPayload());
            assertEquals(result.getPayloadAsString(), "Done");
            callbackCount++;
        }
    }

}
