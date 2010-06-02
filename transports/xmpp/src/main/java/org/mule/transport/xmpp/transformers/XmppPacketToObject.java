/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.xmpp.XmppConnector;
import org.mule.util.StringUtils;

import org.jivesoftware.smack.packet.Message;

public class XmppPacketToObject extends AbstractMessageAwareTransformer
{
    public XmppPacketToObject()
    {
        registerSourceType(Message.class);
        setReturnDataType(DataTypeFactory.create(String.class));
    }

    @Override
    public Object transform(MuleMessage muleMessage, String outputEncoding) throws TransformerException
    {
        Message xmppMessage = (Message) muleMessage.getPayload();
        copySubject(xmppMessage, muleMessage);
        copyThread(xmppMessage, muleMessage);
        copyProperties(xmppMessage, muleMessage);
        return xmppMessage.getBody();
    }

    private void copySubject(Message xmppMessage, MuleMessage muleMessage)
    {
        String subject = xmppMessage.getSubject();
        if (StringUtils.isNotEmpty(subject))
        {
            muleMessage.setProperty(XmppConnector.XMPP_SUBJECT, subject);
        }
    }

    private void copyThread(Message xmppMessage, MuleMessage muleMessage)
    {
        String thread = xmppMessage.getThread();
        if (StringUtils.isNotEmpty(thread))
        {
            muleMessage.setProperty(XmppConnector.XMPP_THREAD, thread);
        }
    }

    private void copyProperties(Message xmppMessage, MuleMessage muleMessage)
    {
        for (String propertyName : xmppMessage.getPropertyNames())
        {
            muleMessage.setProperty(propertyName, xmppMessage.getProperty(propertyName));
        }
    }
}
