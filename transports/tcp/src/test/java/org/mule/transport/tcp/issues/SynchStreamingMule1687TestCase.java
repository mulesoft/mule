/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.issues;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SynchStreamingMule1687TestCase extends DynamicPortTestCase
{
    public static final String TEST_MESSAGE = "Test TCP Request";

    @Override
    protected String getConfigResources()
    {
        return "tcp-synch-streaming-test.xml";
    }

    public void testSendAndRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        MuleMessage request = new DefaultMuleMessage(stream, muleContext);
        MuleMessage message = client.send(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inEcho")).getAddress(), request);
        assertNotNull(message);

        Object payload = message.getPayload();
        assertTrue(payload instanceof InputStream);
        assertEquals("Some value - set to make test ok", message.getPayloadAsString());
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
