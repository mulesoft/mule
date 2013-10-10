/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.jivesoftware.smack.packet.Message;
import org.junit.runners.Parameterized.Parameters;

public class XmppChatAsyncTestCase extends XmppMessageAsyncTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
            {ConfigVariant.SERVICE, "xmpp-chat-async-config-service.xml"},
            {ConfigVariant.FLOW, "xmpp-chat-async-config-flow.xml"}
        });
    }

    public XmppChatAsyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected Message.Type expectedXmppMessageType()
    {
        return Message.Type.chat;
    }

    @Override
    protected void assertXmppMessage(Message message)
    {
        assertEquals(Message.Type.chat, message.getType());
        assertEquals(TEST_MESSAGE, message.getBody());
    }

    @Override
    protected void sendJabberMessageFromNewThread()
    {
        sendChatMessageFromNewThread();
    }
}
