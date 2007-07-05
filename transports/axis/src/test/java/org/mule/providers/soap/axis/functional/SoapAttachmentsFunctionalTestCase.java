/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.functional;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.providers.soap.axis.AxisMessageDispatcher;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

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
        MuleEndpoint ep = new MuleEndpoint(
            "axis:http://localhost:60198/mule/services/testComponent?method=receiveMessageWithAttachments",
            false);
        ep.setManagementContext(managementContext);

        AxisMessageDispatcher client = new AxisMessageDispatcher(ep);
        for (int i = 0; i < iterations; i++)
        {
            UMOMessage msg = new MuleMessage("testPayload");
            File tempFile = File.createTempFile("test", ".att");
            tempFile.deleteOnExit();
            msg.addAttachment("testAttachment", new DataHandler(new FileDataSource(tempFile)));
            MuleSession session = new MuleSession(msg, ((AbstractConnector) ep.getConnector()).getSessionHandler());
            MuleEvent event = new MuleEvent(msg, ep, session, true);
            UMOMessage result = client.send(event);
            assertNotNull(result);
            assertNotNull(result.getPayload());
            assertEquals(result.getPayloadAsString(), "Done");
            callbackCount++;
        }
    }
}
