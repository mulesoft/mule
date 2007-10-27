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

import org.mule.providers.xmpp.XmppConnector;
import org.mule.transformers.AbstractMessageAwareTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;

/**
 * Creates an Xmpp message packet from a UMOMessage
 */
public class ObjectToXmppPacket extends AbstractMessageAwareTransformer
{
    public ObjectToXmppPacket()
    {
        this.registerSourceType(String.class);
        this.registerSourceType(Message.class);
        setReturnClass(Message.class);
    }

    public Object transform(UMOMessage msg, String outputEncoding) throws TransformerException
    {
        Object src = msg.getPayload();
        
        // Make the transformer match its wiki documentation: we accept Messages and Strings.
        // No special treatment for Messages is needed
        if (src instanceof Message)
        {
            return src;
        }
        
        Message result = new Message();

        if (msg.getExceptionPayload() != null)
        {
            result.setError(new XMPPError(503, msg.getExceptionPayload().getMessage()));
        }

        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String name = (String) iterator.next();
            if (name.equals(XmppConnector.XMPP_THREAD))
            {
                result.setThread((String) msg.getProperty(name));
            }
            else if (name.equals(XmppConnector.XMPP_SUBJECT))
            {
                result.setSubject((String) msg.getProperty(name));
            }
            else if (name.equals(XmppConnector.XMPP_FROM))
            {
                result.setFrom((String) msg.getProperty(name));
            }
            else if (name.equals(XmppConnector.XMPP_TO))
            {
                result.setTo((String) msg.getProperty(name));
            }
            else
            {
                result.setProperty(name, msg.getProperty(name));
            }
        }

        // copy the payload. Since it can only be a String (other objects wouldn't be passed in through
        // AbstractTransformer) the following is safe.
        result.setBody((String) src);
        
        return result;
    }
}
