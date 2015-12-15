/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.DataType;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class XmppMessageDispatcherTestCase extends AbstractMuleContextTestCase
{
    @Mock
    private XMPPConnection connection;

    @Mock
    private ChatManager chatManager;

    @Mock
    private Chat chat;

    @Mock
    private XmppConnector connector;

    @Mock
    private OutboundEndpoint endpoint;

    @Mock
    private MuleEvent event;

    @Mock
    private MuleMessage message;

    @Mock
    private Message jabberMessage;

    @Before
    public void before() throws Exception
    {
        when(chatManager.createChat(anyString(), (MessageListener) eq(null))).thenReturn(chat);
        when(connection.getChatManager()).thenReturn(chatManager);

        when(connector.getConversationFactory()).thenReturn(new XmppConversationFactory());
        when(connector.getXmppConnection()).thenReturn(connection);

        when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("xmpp://CHAT/localhost", muleContext));
        when(endpoint.getConnector()).thenReturn(connector);

        when(event.getMessage()).thenReturn(message);
        when(message.getPayload((DataType<Message>) anyObject())).thenReturn(jabberMessage);
    }

    @Test
    public void sendMessage() throws Exception
    {
        XmppMessageDispatcher dispatcher = new XmppMessageDispatcher(endpoint);
        dispatcher.connect();

        when(connection.isConnected()).thenReturn(true);
        dispatcher.sendMessage(event);

        verify(connection, never()).connect();
        verify(chat).sendMessage(jabberMessage);
    }

    @Test
    public void sessionCloseOnServer() throws Exception
    {
        XmppMessageDispatcher dispatcher = new XmppMessageDispatcher(endpoint);
        dispatcher.connect();

        when(connection.isConnected()).thenReturn(false);
        dispatcher.sendMessage(event);

        verify(connection).connect();
        verify(chat).sendMessage(jabberMessage);
    }

    @Test(expected = IllegalStateException.class)
    public void serverDownWhenSending() throws Exception
    {
        XmppMessageDispatcher dispatcher = new XmppMessageDispatcher(endpoint);
        dispatcher.connect();

        doThrow(IllegalStateException.class).when(chat).sendMessage(jabberMessage);
        dispatcher.sendMessage(event);
    }
}
