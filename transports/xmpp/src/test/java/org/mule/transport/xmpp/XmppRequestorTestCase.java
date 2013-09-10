/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.jivesoftware.smack.packet.Message;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class XmppRequestorTestCase extends AbstractXmppTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{ { ConfigVariant.FLOW, "" } });
    }

    public XmppRequestorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void requestMessage() throws Exception
    {
        sendNormalMessageFromNewThread();

        String url = "xmpp://MESSAGE/mule2@localhost?exchangePattern=request-response";
        doRequest(url, Message.Type.normal);
    }

    @Test
    public void requestChat() throws Exception
    {
        sendChatMessageFromNewThread();

        String url = "xmpp://CHAT/mule2@localhost?exchangePattern=request-response";
        doRequest(url, Message.Type.chat);
    }

    protected void doRequest(String url, Message.Type expectedType) throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage muleMessage = client.request(url, RECEIVE_TIMEOUT);
        assertNotNull(muleMessage);

        Message xmppMessage = (Message)muleMessage.getPayload();
        assertEquals(expectedType, xmppMessage.getType());
        assertEquals(TEST_MESSAGE, xmppMessage.getBody());
    }
}
