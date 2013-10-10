/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.NullPayload;
import org.mule.transport.xmpp.JabberSender.Callback;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XmppMessageSyncTestCase extends AbstractXmppTestCase
{
    
    protected static final long JABBER_SEND_THREAD_SLEEP_TIME = 1000;
    private static final String RECEIVE_SERVICE_NAME = "receiveFromJabber";
        
    @Override
    protected String getXmppConfigResources()
    {
        return "xmpp-message-sync-config.xml";
    }

    @Test
    public void testSendSync() throws Exception
    {   
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://in", TEST_MESSAGE, null);
        assertNotNull(reply);
        assertEquals(NullPayload.getInstance(), reply.getPayload());
        
        Packet packet = jabberClient.receive(RECEIVE_TIMEOUT);
        assertReceivedPacketEqualsMessageSent(packet);
    }
    
    @Test
    public void testReceiveSync() throws Exception
    {
        startService(RECEIVE_SERVICE_NAME);
        
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

    @Test
    public void testRequestSync() throws Exception
    {
        doTestRequest("xmpp://MESSAGE/mule2@localhost?exchangePattern=request-response");
    }
    
    protected void doTestRequest(String url) throws Exception
    {
        sendJabberMessageFromNewThread();

        MuleClient client = new MuleClient(muleContext);
        MuleMessage muleMessage = client.request(url, RECEIVE_TIMEOUT);
        assertNotNull(muleMessage);

        Message xmppMessage = (Message) muleMessage.getPayload();
        assertEquals(expectedXmppMessageType(), xmppMessage.getType());
        assertEquals(TEST_MESSAGE, xmppMessage.getBody());
    }

    protected Message.Type expectedXmppMessageType()
    {
        return Message.Type.normal;
    }

    protected void sendJabberMessageFromNewThread()
    {
        JabberSender sender = new JabberSender(new Callback()
        {
            public void doit() throws Exception
            {
                Thread.sleep(JABBER_SEND_THREAD_SLEEP_TIME);
                jabberClient.sendMessage(muleJabberUserId, TEST_MESSAGE);
            }
        });
        startSendThread(sender);
    }
}
