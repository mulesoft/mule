/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.transport.xmpp.JabberSender.Callback;

import java.util.Arrays;
import java.util.Collection;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.junit.Ignore;
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

    @Ignore("requesting should be done in a completely different test case that does not require an initially stopped service")
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
            @Override
            public void doit() throws Exception
            {
                Thread.sleep(JABBER_SEND_THREAD_SLEEP_TIME);
                jabberClient.sendChatMessage(muleJabberUserId, TEST_MESSAGE);
            }
        });
        startSendThread(sender);
    }
}
