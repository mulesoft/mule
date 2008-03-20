/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.xmpp.XmppConnector;

public class XmppMessages extends MessageFactory
{
    private static final XmppMessages factory = new XmppMessages();
    
    private static final String BUNDLE_PATH = getBundlePath(XmppConnector.XMPP);

    public static Message noRecipientInUri()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message nicknameMustBeSet()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }
}


