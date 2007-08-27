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

import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

import org.jivesoftware.smack.packet.Message;

public class XmppPacketToObject extends AbstractEventAwareTransformer
{

    public XmppPacketToObject()
    {
        registerSourceType(Message.class);
        setReturnClass(String.class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        Message message = (Message) src;
        UMOMessage msg = context.getMessage();

        for (Iterator iterator = message.getPropertyNames(); iterator.hasNext();)
        {
            String name = (String) iterator.next();
            msg.setProperty(name, message.getProperty(name));
        }

        return message.getBody();
    }

}
