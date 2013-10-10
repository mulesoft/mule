/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.MuleTestUtils;
import org.mule.transport.AbstractMessageReceiverTestCase;
import org.mule.util.FileUtils;

import com.mockobjects.dynamic.Mock;

import java.io.File;

public class FileMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    File read = FileUtils.newFile("testcasedata/read");
    File move = FileUtils.newFile("testcasedata/move");
    Mock session = MuleTestUtils.getMockSession();

    public void testReceiver() throws Exception
    {
        // FIX A bit hard testing receive from a unit simple as we need to reg
        // listener etc.
        // file endpoint functions tests for this
    }

    public MessageReceiver getMessageReceiver() throws Exception
    {
        endpoint.getConnector().start();
        Mock mockComponent = new Mock(Service.class);
        mockComponent.expectAndReturn("getInboundRouter", null);
        mockComponent.expectAndReturn("getResponseRouter", null);
        read.deleteOnExit();
        move.deleteOnExit();

        return new FileMessageReceiver(endpoint.getConnector(), (Service)mockComponent.proxy(),
            endpoint, read.getAbsolutePath(), move.getAbsolutePath(), null, 1000);
    }

    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("file://./simple");
    }
}
