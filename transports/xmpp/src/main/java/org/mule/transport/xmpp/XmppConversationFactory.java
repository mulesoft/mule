/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.mule.api.MuleRuntimeException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.xmpp.i18n.XmppMessages;

/**
 * A factory that creates {@link XmppConversation} instances based on the endpoint's configuration.
 */
public class XmppConversationFactory
{
    public XmppConversation create(ImmutableEndpoint endpoint)
    {
        String host = endpoint.getEndpointURI().getHost();
        XmppMessageType type = XmppMessageType.valueOf(host);
        
        switch (type)
        {
            case MESSAGE:
                return createMessageConversation(endpoint);
            
            case CHAT:
                return createChatConversation(endpoint);

            case GROUPCHAT:
                return createGroupchatConversation(endpoint);
        }                

        // we should never get here as valueOf on the enum above will choke if you pass
        // in an invalid string
        throw new MuleRuntimeException(XmppMessages.invalidConversationType(type));
    }

    protected XmppConversation createMessageConversation(ImmutableEndpoint endpoint)
    {
        return new XmppMessageConversation(endpoint);
    }

    protected XmppConversation createChatConversation(ImmutableEndpoint endpoint)
    {
        return new XmppChatConversation(endpoint);
    }

    protected XmppConversation createGroupchatConversation(ImmutableEndpoint endpoint)
    {
        return new XmppMultiUserChatConversation(endpoint);
    }
}


