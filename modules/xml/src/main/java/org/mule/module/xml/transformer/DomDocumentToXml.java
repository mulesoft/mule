/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;

/**
 * <code>DomDocumentToXml</code> Transform a org.w3c.dom.Document to XML String
 */
public class DomDocumentToXml extends AbstractXmlTransformer implements DiscoverableTransformer
{
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public DomDocumentToXml()
    {
        setReturnDataType(DataTypeFactory.XML_STRING);
    }

    @Override
    public Object transformMessage(MuleMessage message, String encoding) throws TransformerException
    {
        Object src = message.getPayload();
        try
        {
            // We now offer XML in byte OR String form.
            // String remains the default like before.
            if (byte[].class.equals(returnType))
            {
                return convertToBytes(src, encoding);
            }
            else
            {
                return convertToText(src, encoding);
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
