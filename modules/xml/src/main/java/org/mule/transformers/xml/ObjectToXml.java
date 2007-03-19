/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>ObjectToXml</code> converts any object to xml using Xstream. Xstream uses
 * some cleaver tricks so objects that get marshalled to xml do not need to implement
 * any interfaces including Serializable and you don't even need to specify a default
 * constructor. If <code>UMOMessage</code> is added as a source type on this
 * transformer then the UMOMessage will be serialised. This is useful for transports
 * such as tcp where the message headers would normally be lost.
 */

public class ObjectToXml extends AbstractXStreamTransformer
{

    public ObjectToXml()
    {
        registerSourceType(Object.class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        /*
         * If the UMOMessage source type has been registered that we can assume that
         * the whole message is to be serialised to Xml, not just the payload. This
         * can be useful for protocols such as tcp where the protocol does not
         * support headers, thus the whole messgae needs to be serialized
         */
        if (isSourceTypeSupported(UMOMessage.class, true) && context != null)
        {
            return getXStream().toXML(context.getMessage());
        }
        else
        {
            return getXStream().toXML(src);
        }
    }
}
