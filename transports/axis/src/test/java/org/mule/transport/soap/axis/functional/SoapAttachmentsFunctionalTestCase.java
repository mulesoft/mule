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
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleSession;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.AbstractConnector;
import org.mule.transport.soap.axis.AxisMessageDispatcher;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class SoapAttachmentsFunctionalTestCase extends FunctionalTestCase
{
    private static final int SEND_COUNT = 5;
    
    private int callbackCount = 0;

    protected String getConfigResources()
    {
        return "axis-soap-attachments.xml";
    }

    public void testSend() throws Exception
    {
        sendTestData(SEND_COUNT);
        assertEquals(SEND_COUNT, callbackCount);
    }

    protected void sendTestData(int iterations) throws Exception
    {
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "axis:http://localhost:60198/mule/services/testComponent?method=receiveMessageWithAttachments");

        AxisMessageDispatcher client = new AxisMessageDispatcher(ep);
        for (int i = 0; i < iterations; i++)
        {
            MuleMessage msg = new DefaultMuleMessage("testPayload");
            File tempFile = File.createTempFile("test", ".att");
            tempFile.deleteOnExit();
            msg.addAttachment("testAttachment", new DataHandler(new FileDataSource(tempFile)));
            DefaultMuleSession session = new DefaultMuleSession(msg, ((AbstractConnector) ep.getConnector()).getSessionHandler());
            DefaultMuleEvent event = new DefaultMuleEvent(msg, ep, session, true);
            MuleMessage result = client.send(event);
            assertNotNull(result);
            assertNotNull(result.getPayload());
            assertEquals(result.getPayloadAsString(), "Done");
            callbackCount++;
        }
    }
}
