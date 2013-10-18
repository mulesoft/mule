/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.xmpp.XmppConnector;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;

/**
 * Creates an Xmpp message packet from a MuleMessage
 */
public class ObjectToXmppPacket extends AbstractMessageTransformer
{
    public ObjectToXmppPacket()
    {
        DataType<Message> messageDataType = DataTypeFactory.create(Message.class);

        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(messageDataType);
        setReturnDataType(messageDataType);
    }

    @Override
    public Object transformMessage(MuleMessage muleMessage, String outputEncoding)
    {
        Object src = muleMessage.getPayload();
        
        // Make the transformer match its wiki documentation: we accept Messages and Strings.
        // No special treatment for Messages is needed
        if (src instanceof Message)
        {
            return src;
        }
        
        Message result = new Message();

        if (muleMessage.getExceptionPayload() != null)
        {
            result.setError(
                new XMPPError(XMPPError.Condition.service_unavailable,
                    muleMessage.getExceptionPayload().getMessage()));
        }

        for (String propertyName : muleMessage.getOutboundPropertyNames())
        {
            if (propertyName.equals(XmppConnector.XMPP_THREAD))
            {
                result.setThread((String) muleMessage.getOutboundProperty(propertyName));
            }
            else if (propertyName.equals(XmppConnector.XMPP_SUBJECT))
            {
                result.setSubject((String) muleMessage.getOutboundProperty(propertyName));
            }
            else if (propertyName.equals(XmppConnector.XMPP_FROM))
            {
                result.setFrom((String) muleMessage.getOutboundProperty(propertyName));
            }
            else if (propertyName.equals(XmppConnector.XMPP_TO))
            {
                result.setTo((String) muleMessage.getOutboundProperty(propertyName));
            }
            else
            {
                result.setProperty(propertyName, muleMessage.<Object>getOutboundProperty(propertyName));
            }
        }

        // copy the payload. Since it can only be a String (other objects wouldn't be passed in through
        // AbstractTransformer) the following is safe.
        result.setBody((String) src);
        
        return result;
    }
}
