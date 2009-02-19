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
import org.mule.config.i18n.MessageFactory;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.module.xml.util.XMLUtils;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.w3c.dom.Document;

public class XmlToXMLStreamReader extends AbstractXmlTransformer implements DiscoverableTransformer
{

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;
    private boolean reversible;
    
    public XmlToXMLStreamReader()
    {
        super();
        registerSourceType(Source.class);
        registerSourceType(InputStream.class);
        registerSourceType(Document.class);
        registerSourceType(byte[].class);
        registerSourceType(String.class);

        setReturnClass(XMLStreamReader.class);
        
    }

    protected Object doTransform(Object payload, String encoding) throws TransformerException
    {
        try 
        {
            XMLStreamReader xsr = XMLUtils.toXMLStreamReader(getXMLInputFactory(), payload);
            if (xsr == null)
            {
                throw new TransformerException(MessageFactory
                    .createStaticMessage("Unable to convert " + payload.getClass() + " to XMLStreamReader."));
            }
        
            if (reversible && !(xsr instanceof ReversibleXMLStreamReader))
            {
                return new ReversibleXMLStreamReader(xsr);
            }
            else
            {
                return xsr;
            }
        }
        catch (XMLStreamException e)
        {
            throw new TransformerException(this, e);
        }
    }

    public boolean isReversible()
    {
        return reversible;
    }

    public void setReversible(boolean reversible)
    {
        this.reversible = reversible;
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