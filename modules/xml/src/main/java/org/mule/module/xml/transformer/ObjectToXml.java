/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;

/**
 * <code>ObjectToXml</code> converts any object to XML using Xstream. Xstream uses
 * some clever tricks so objects that get marshalled to XML do not need to implement
 * any interfaces including Serializable and you don't even need to specify a default
 * constructor. If <code>MuleMessage</code> is configured as a source type on this
 * transformer by calling <code>setAcceptMuleMessage(true)</code> then the MuleMessage
 * will be serialised. This is useful for transports such as TCP where the message
 * headers would normally be lost.
 */

public class ObjectToXml extends AbstractXStreamTransformer
{

    public ObjectToXml()
    {
        this.registerSourceType(DataTypeFactory.OBJECT);
        this.setReturnDataType(DataTypeFactory.STRING);
    }

    public boolean isAcceptMuleMessage()
    {
        return this.sourceTypes.contains(MULE_MESSAGE_DATA_TYPE);
    }

    public void setAcceptMuleMessage(boolean value)
    {
        if (value)
        {
            this.registerSourceType(DataTypeFactory.MULE_MESSAGE);
        }
        else
        {
            this.unregisterSourceType(DataTypeFactory.MULE_MESSAGE);
        }
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object src = message.getPayload();
        /*
         * If the MuleMessage source type has been registered that we can assume that
         * the whole message is to be serialised to Xml, not just the payload. This
         * can be useful for protocols such as tcp where the protocol does not
         * support headers, thus the whole messgae needs to be serialized
         */
        if (this.isAcceptMuleMessage())
        {
            src = message;
        }
        return this.getXStream().toXML(src);
    }
}
