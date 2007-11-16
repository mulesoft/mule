/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>ObjectToXml</code> converts any object to XML using Xstream. Xstream uses
 * some clever tricks so objects that get marshalled to XML do not need to implement
 * any interfaces including Serializable and you don't even need to specify a default
 * constructor. If <code>UMOMessage</code> is configured as a source type on this
 * transformer by calling <code>setAcceptUMOMessage(true)</code> then the UMOMessage
 * will be serialised. This is useful for transports such as TCP where the message
 * headers would normally be lost.
 */

public class ObjectToXml extends AbstractXStreamTransformer
{

    public ObjectToXml()
    {
        this.registerSourceType(Object.class);
        this.setReturnClass(String.class);
    }

    public boolean isAcceptUMOMessage()
    {
        return this.sourceTypes.contains(UMOMessage.class);
    }

    public void setAcceptUMOMessage(boolean value)
    {
        if (value)
        {
            this.registerSourceType(UMOMessage.class);
        }
        else
        {
            this.unregisterSourceType(UMOMessage.class);
        }
    }

    public Object transform(UMOMessage message, String outputEncoding) throws TransformerException
    {
        Object src = message.getPayload();
        /*
         * If the UMOMessage source type has been registered that we can assume that
         * the whole message is to be serialised to Xml, not just the payload. This
         * can be useful for protocols such as tcp where the protocol does not
         * support headers, thus the whole messgae needs to be serialized
         */
        if (this.isAcceptUMOMessage())
        {
            src = message;
        }
        return this.getXStream().toXML(src);
    }
}
