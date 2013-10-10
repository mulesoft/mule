/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
