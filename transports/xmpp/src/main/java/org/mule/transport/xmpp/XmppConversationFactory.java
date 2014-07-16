/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

            case dynamic:
                // this cannot happen
                throw new IllegalStateException();
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


