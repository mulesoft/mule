/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.issues;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SynchStreamingMule1687TestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "tcp-synch-streaming-test.xml";
    }

    @Test
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

}
