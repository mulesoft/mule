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

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.NullPayload;
import org.mule.transport.xmpp.JabberSender.Callback;
import org.mule.util.UUID;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class XmppMucSyncTestCase extends AbstractXmppTestCase
{
    protected static final long JABBER_SEND_THREAD_SLEEP_TIME = 1000;
    private static final String RECEIVE_SERVICE_NAME = "receiveFromJabber";
    private static final long SHORT_RETRIEVE_TIMEOUT = 100;

    private final String testMessage = UUID.getUUID().toString();

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "xmpp-muc-sync-config-service.xml"},
            {ConfigVariant.FLOW, "xmpp-muc-sync-config-flow.xml"}
        });
    }

    public XmppMucSyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        Properties properties = (Properties) muleContext.getRegistry().lookupObject("properties");
        String chatroom = properties.getProperty("chatroom");
        assertNotNull(chatroom);

        jabberClient.joinGroupchat(chatroom);
    }

    @Test
    public void testSendSync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("vm://in", testMessage, null);
        assertNotNull(reply);

        assertEquals(NullPayload.getInstance(), reply.getPayload());

        Packet packet = jabberClient.receive(RECEIVE_TIMEOUT);
        // The groupchat may have a backlog of messages whis is sent before our input
        // is transmitted. Poll the entire groupchat history
        boolean inputSeen = false;
        packet = jabberClient.receive(SHORT_RETRIEVE_TIMEOUT);
        while (packet != null)
        {
            String payload = ((Message) packet).getBody();
            if (payload.equals(testMessage))
            {
                inputSeen = true;
                break;
            }

            packet = jabberClient.receive(SHORT_RETRIEVE_TIMEOUT);
        }
        assertTrue(inputSeen);
    }

    @Test
    public void testReceiveSync() throws Exception
    {
        Latch receiveLatch = new Latch();
        setupTestServiceComponent(receiveLatch);

        sendMucMessageFromNewThread();
        assertTrue(receiveLatch.await(60, TimeUnit.SECONDS));
    }

    private void setupTestServiceComponent(Latch receiveLatch) throws Exception
    {
        Object testComponent = getComponent(RECEIVE_SERVICE_NAME);
        assertTrue(testComponent instanceof FunctionalTestComponent);
        FunctionalTestComponent component = (FunctionalTestComponent) testComponent;

        XmppGroupchatCallback callback = new XmppGroupchatCallback(receiveLatch);
        component.setEventCallback(callback);
    }

    protected Message.Type expectedXmppMessageType()
    {
        return Message.Type.groupchat;
    }

    protected void sendMucMessageFromNewThread()
    {
        JabberSender sender = new JabberSender(new Callback()
        {
            @Override
            public void doit() throws Exception
            {
                Thread.sleep(JABBER_SEND_THREAD_SLEEP_TIME);
                jabberClient.sendGroupchatMessage(testMessage);
            }
        });
        startSendThread(sender);
    }

    private class XmppGroupchatCallback implements EventCallback
    {
        private Latch latch;

        public XmppGroupchatCallback(Latch latch)
        {
            super();
            this.latch = latch;
        }

        @Override
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            MuleMessage muleMessage = context.getMessage();
            Object payload = muleMessage.getPayload();
            assertTrue(payload instanceof Message);

            Message jabberMessage = (Message) payload;
            if (jabberMessage.getBody().equals(testMessage))
            {
                latch.countDown();
            }
        }
    }
}
