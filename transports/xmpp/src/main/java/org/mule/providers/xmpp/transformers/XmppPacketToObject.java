/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp.transformers;

import org.mule.transformers.AbstractMessageAwareTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

import org.jivesoftware.smack.packet.Message;

public class XmppPacketToObject extends AbstractMessageAwareTransformer
{

    public XmppPacketToObject()
    {
        registerSourceType(Message.class);
        setReturnClass(String.class);
    }

    public Object transform(UMOMessage message, String outputEncoding) throws TransformerException
    {
        Message xmppMessage = (Message) message.getPayload();

        for (Iterator iterator = xmppMessage.getPropertyNames(); iterator.hasNext();)
        {
            String name = (String) iterator.next();
            message.setProperty(name, xmppMessage.getProperty(name));
        }

        return xmppMessage.getBody();
    }

}
