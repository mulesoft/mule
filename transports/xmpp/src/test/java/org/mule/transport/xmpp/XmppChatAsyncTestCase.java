/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.packet.Message;

public class XmppChatAsyncTestCase extends XmppMessageAsyncTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "xmpp-chat-async-config-flow.xml";
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
