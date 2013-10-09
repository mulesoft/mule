/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import org.jivesoftware.smack.packet.Message;

/**
 * Allows Mule events to be sent over Xmpp
 */
public class XmppMessageDispatcher extends AbstractMessageDispatcher
{
    private final XmppConnector xmppConnector;
    private XmppConversation conversation;

    public XmppMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        xmppConnector = (XmppConnector) endpoint.getConnector();
        conversation = xmppConnector.getConversationFactory().create(endpoint);
    }

    @Override
    protected void doConnect() throws Exception
    {
        conversation.connect();
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        conversation.disconnect();
    }

    @Override
    protected void doDispose()
    {
        conversation = null;
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        sendMessage(event);
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        sendMessage(event);

        // TODO xmpp: even sync endpoints do not wait for a reply. Look at how the JMS transport handles replies, use reply handler
//        if (returnResponse(event, false))
//        {
//            Message response = conversation.receive(event.getTimeout());
//
////            if (groupChat != null)
////            {
////                response = groupChat.nextMessage(event.getTimeout());
////            }
//
//            if (response != null)
//            {
//                return createMuleMessage(response);
//            }
//        }
        return new DefaultMuleMessage(NullPayload.getInstance(), xmppConnector.getMuleContext());
    }

    protected void sendMessage(MuleEvent event) throws Exception
    {
        Message jabberMessage = event.getMessage().getPayload(DataTypeFactory.create(Message.class));
        conversation.dispatch(jabberMessage);

        if (logger.isDebugEnabled())
        {
            String recipient = XmppConnector.getRecipient(endpoint);
            logger.debug("Message \"" + jabberMessage.getBody()
                + "\" successfully sent to " + recipient);
        }
    }
}
