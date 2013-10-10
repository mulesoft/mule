/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


