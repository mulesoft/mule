/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
