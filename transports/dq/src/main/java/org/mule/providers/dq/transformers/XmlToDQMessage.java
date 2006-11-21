/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.dq.transformers;

import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mule.providers.dq.DQMessage;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code> XmlToDQMessage</code> will convert an XML string to a DQMessage.
 */
public class XmlToDQMessage extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6677408209819658768L;

    /**
     * The contructor
     */
    public XmlToDQMessage()
    {
        registerSourceType(String.class);
        setReturnClass(DQMessage.class);
    }

    /**
     * @see org.mule.transformers.AbstractTransformer#doTransform(Object, String)
     */
    public final Object doTransform(final Object src, String encoding) throws TransformerException
    {
        try
        {
            DQMessage msg = new DQMessage();
            Document document = DocumentHelper.parseText((String)src);

            for (Iterator i = document.getRootElement().elementIterator(); i.hasNext();)
            {
                Element element = (Element)i.next();
                String name = element.attributeValue(DQMessage.XML_NAME);
                String value = element.getTextTrim();
                msg.addEntry(name, value);
            }

            return msg;

        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
