/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.xmpp.XmppConnector;
import org.mule.transport.xmpp.XmppMessageType;

public class XmppMessages extends MessageFactory
{
    private static final XmppMessages factory = new XmppMessages();
    
    private static final String BUNDLE_PATH = getBundlePath(XmppConnector.XMPP);

    public static Message noRecipientInUri()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message invalidConversationType(XmppMessageType type)
    {
        return factory.createMessage(BUNDLE_PATH, 3, type.name());
    }

    public static Message noMessageTypeInUri()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }

    public static Message invalidMessageTypeInUri()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }
}


