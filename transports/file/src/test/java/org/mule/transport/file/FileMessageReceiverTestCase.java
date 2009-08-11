/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        mockComponent.expectAndReturn("getResponseRouter", null);
        read.deleteOnExit();
        move.deleteOnExit();

        return new FileMessageReceiver(endpoint.getConnector(), (Service)mockComponent.proxy(),
            endpoint, read.getAbsolutePath(), move.getAbsolutePath(), null, 1000);
    }

    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("file://./simple");
    }
}
