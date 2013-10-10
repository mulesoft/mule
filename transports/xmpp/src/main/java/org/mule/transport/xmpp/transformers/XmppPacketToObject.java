/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.xmpp.XmppConnector;
import org.mule.util.StringUtils;

import org.jivesoftware.smack.packet.Message;

public class XmppPacketToObject extends AbstractMessageTransformer
{
    public XmppPacketToObject()
    {
        registerSourceType(DataTypeFactory.create(Message.class));
        setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    public Object transformMessage(MuleMessage muleMessage, String outputEncoding)
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
            muleMessage.setProperty(XmppConnector.XMPP_SUBJECT, subject, PropertyScope.INBOUND);
        }
    }

    private void copyThread(Message xmppMessage, MuleMessage muleMessage)
    {
        String thread = xmppMessage.getThread();
        if (StringUtils.isNotEmpty(thread))
        {
            muleMessage.setOutboundProperty(XmppConnector.XMPP_THREAD, thread);
        }
    }

    private void copyProperties(Message xmppMessage, MuleMessage muleMessage)
    {
        for (String propertyName : xmppMessage.getPropertyNames())
        {
            muleMessage.setOutboundProperty(propertyName, xmppMessage.getProperty(propertyName));
        }
    }
}
