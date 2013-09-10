/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.NullPayload;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class XmppMessageSyncTestCase extends AbstractXmppTestCase
{
    protected static final long JABBER_SEND_THREAD_SLEEP_TIME = 1000;
    private static final String RECEIVE_SERVICE_NAME = "receiveFromJabber";

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
            {ConfigVariant.SERVICE, "xmpp-message-sync-config-service.xml"},
            {ConfigVariant.FLOW, "xmpp-message-sync-config-flow.xml"}
        });
    }

    public XmppMessageSyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSendSync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("vm://in", TEST_MESSAGE, null);
        assertNotNull(reply);
        assertEquals(NullPayload.getInstance(), reply.getPayload());

        Packet packet = jabberClient.receive(RECEIVE_TIMEOUT);
        assertReceivedPacketEqualsMessageSent(packet);
    }

    @Test
    public void testReceiveSync() throws Exception
    {
        Latch receiveLatch = new Latch();
        setupTestServiceComponent(receiveLatch);

        sendJabberMessageFromNewThread();
        assertTrue(receiveLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private void setupTestServiceComponent(Latch receiveLatch) throws Exception
    {
        Object testComponent = getComponent(RECEIVE_SERVICE_NAME);
        assertTrue(testComponent instanceof FunctionalTestComponent);
        FunctionalTestComponent component = (FunctionalTestComponent) testComponent;

        XmppCallback callback = new XmppCallback(receiveLatch, expectedXmppMessageType());
        component.setEventCallback(callback);
    }

    protected  void sendJabberMessageFromNewThread()
    {
        sendNormalMessageFromNewThread();
    }

    protected Message.Type expectedXmppMessageType()
    {
        return Message.Type.normal;
    }
}
