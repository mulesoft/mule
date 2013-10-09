/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import java.util.Arrays;
import java.util.Collection;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.junit.runners.Parameterized.Parameters;

public class XmppChatSyncTestCase extends XmppMessageSyncTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "xmpp-chat-sync-config-service.xml"},
            {ConfigVariant.FLOW, "xmpp-chat-sync-config-flow.xml"}
        });
    }

    public XmppChatSyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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
