/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

public class SynchStreamingMule1687TestCase extends FunctionalTestCase
{
    public static final String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "tcp-synch-streaming-test.xml";
    }

    @Test
    public void testSendAndRequest() throws Exception
    {
        MuleClient client = muleContext.getClient();
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        MuleMessage request = new DefaultMuleMessage(stream, muleContext);
        MuleMessage message = client.send(((InboundEndpoint) muleContext.getRegistry().lookupObject("inEcho")).getAddress(), request);
        assertNotNull(message);

        Object payload = message.getPayload();
        assertTrue(payload instanceof InputStream);
        assertEquals("Some value - set to make test ok", message.getPayloadAsString());
    }
}
