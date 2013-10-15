/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import java.util.Arrays;
import java.util.Collection;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.junit.runners.Parameterized.Parameters;
import org.mule.transport.xmpp.JabberSender.Callback;

public class XmppChatSyncTestCase extends XmppMessageSyncTestCase
{
    
    public XmppChatSyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, AbstractXmppTestCase.COMMON_CONFIG + "," + "xmpp-chat-sync-config-service.xml"},
            {ConfigVariant.FLOW, AbstractXmppTestCase.COMMON_CONFIG + "," + "xmpp-chat-sync-config-flow.xml"}
        });
    }

    @Override
    public void testRequestSync() throws Exception
    {
        doTestRequest("xmpp://CHAT/mule2@localhost?exchangePattern=request-response");
    }

    @Override
    protected Type expectedXmppMessageType()
    {
        return Message.Type.chat;
    }
    
    @Override
    protected void sendJabberMessageFromNewThread()
    {
        JabberSender sender = new JabberSender(new Callback()
        {
            public void doit() throws Exception
            {
                Thread.sleep(JABBER_SEND_THREAD_SLEEP_TIME);
                jabberClient.sendChatMessage(muleJabberUserId, TEST_MESSAGE);
            }
        });
        startSendThread(sender);
    }
}
