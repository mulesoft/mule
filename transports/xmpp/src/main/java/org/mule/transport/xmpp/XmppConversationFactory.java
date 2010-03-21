/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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
        String type = endpoint.getEndpointURI().getHost();
        if (XmppConnector.CONVERSATION_TYPE_MESSAGE.equals(type))
        {
            return createMessageConversation(endpoint);
        }
        else if (XmppConnector.CONVERSATION_TYPE_CHAT.equals(type))
        {
            return createChatConversation(endpoint);
        }
        else if (XmppConnector.CONVERSATION_TYPE_GROUPCHAT.equals(type))
        {
            return createGroupchatConversation(endpoint);
        }
        else
        {
            throw new MuleRuntimeException(XmppMessages.invalidConversationType(type));
        }
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


