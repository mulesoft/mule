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
import org.mule.module.xml.util.XMLUtils;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.w3c.dom.Document;

/**
 * <code>XmlToDomDocument</code> transforms a XML String to org.w3c.dom.Document.
 */
public class XmlToDomDocument extends AbstractXmlTransformer implements DiscoverableTransformer
{
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    @Override
    public Object transformMessage(MuleMessage message, String encoding) throws TransformerException
    {
        Object src = message.getPayload();
        try
        {
            Source sourceDoc = XMLUtils.toXmlSource(getXMLInputFactory(), isUseStaxSource(), src);
            if (sourceDoc == null)
            {
                return null;
            }

            if (XMLStreamReader.class.equals(returnType))
            {
                return getXMLInputFactory().createXMLStreamReader(sourceDoc);
            }
            else if (returnType.getType().isAssignableFrom(sourceDoc.getClass()))
            {
                return sourceDoc;
            }

            // If returnClass is not set, assume W3C DOM
            // This is the original behaviour
            ResultHolder holder = getResultHolder(returnType.getType());
            if (holder == null)
            {
                holder = getResultHolder(Document.class);
            }

            Transformer idTransformer = XMLUtils.getTransformer();
            idTransformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            idTransformer.transform(sourceDoc, holder.getResult());

            return holder.getResultObject();
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
