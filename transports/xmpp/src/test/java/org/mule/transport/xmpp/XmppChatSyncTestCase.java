/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;

public class XmppChatSyncTestCase extends XmppMessageSyncTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "xmpp-chat-sync-config-flow.xml";
    }

    @Override
    protected  void sendJabberMessageFromNewThread()
    {
        sendChatMessageFromNewThread();
    }

    @Override
    protected Type expectedXmppMessageType()
    {
        return Message.Type.chat;
    }
}
