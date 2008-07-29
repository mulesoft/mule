/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.transformer;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.util.XMLUtils;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.w3c.dom.Document;

/** <code>XmlToDomDocument</code> transforms a XML String to org.w3c.dom.Document. */
public class XmlToDomDocument extends AbstractXmlTransformer implements DiscoverableTransformer
{
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            Source sourceDoc = getXmlSource(src);
            if (sourceDoc == null)
            {
                return null;
            }

            if (XMLStreamReader.class.equals(returnClass))
            {
                return getXMLInputFactory().createXMLStreamReader(sourceDoc);
            }
            else if (returnClass.isAssignableFrom(sourceDoc.getClass()))
            {
                return sourceDoc;
            }
            
            // If returnClass is not set, assume W3C DOM
            // This is the original behaviour
            ResultHolder holder = getResultHolder(returnClass);
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
