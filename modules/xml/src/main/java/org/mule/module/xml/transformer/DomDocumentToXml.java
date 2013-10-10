/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
