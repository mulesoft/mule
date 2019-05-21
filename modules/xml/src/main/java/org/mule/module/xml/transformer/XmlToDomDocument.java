/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import static javax.xml.transform.OutputKeys.ENCODING;
import static org.mule.module.xml.util.XMLUtils.getTransformer;
import static org.mule.module.xml.util.XMLUtils.toXmlSource;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.dom4j.io.DocumentResult;
import org.dom4j.io.SAXContentHandler;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;

/**
 * <code>XmlToDomDocument</code> transforms a XML String to org.w3c.dom.Document.
 */
public class XmlToDomDocument extends AbstractXmlTransformer implements DiscoverableTransformer
{
    private int priorityWeighting = DEFAULT_PRIORITY_WEIGHTING;

    @Override
    public Object transformMessage(MuleMessage message, String encoding) throws TransformerException
    {
        Object src = message.getPayload();
        try
        {
            Source sourceDoc = toXmlSource(getXMLInputFactory(), isUseStaxSource(), src);
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

            Result result = holder.getResult();

            if (result instanceof DocumentResult)
            {
                DocumentResult dr = (DocumentResult) holder.getResult();
                ContentHandler contentHandler = dr.getHandler();
                if (contentHandler instanceof SAXContentHandler)
                {
                    //The following code is used to avoid the splitting
                    //of text inside DOM elements.
                    ((SAXContentHandler) contentHandler).setMergeAdjacentText(true);
                }
            }

            Transformer idTransformer = getTransformer();
            idTransformer.setOutputProperty(ENCODING, encoding);
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
