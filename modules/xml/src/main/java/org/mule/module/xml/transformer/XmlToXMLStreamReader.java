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

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.module.xml.stax.StaxSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlToXMLStreamReader extends AbstractXmlTransformer
{
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
            XMLStreamReader xsr;
            
            if (payload instanceof StaxSource)
            {
                xsr = ((StaxSource) payload).getXMLStreamReader();
            }
            else if (payload instanceof Source)
            {
                xsr = getXMLInputFactory().createXMLStreamReader((Source) payload);
            }
            else if (payload instanceof Document)
            {
                xsr = getXMLInputFactory().createXMLStreamReader(new DOMSource((Node) payload));
            }
            else if (payload instanceof InputStream)
            {
                xsr = getXMLInputFactory().createXMLStreamReader((InputStream) payload);
            }
            else if (payload instanceof String)
            {
                xsr = getXMLInputFactory().createXMLStreamReader(new StringReader((String) payload));
            }
            else if (payload instanceof byte[])
            {
                xsr = getXMLInputFactory().createXMLStreamReader(new ByteArrayInputStream((byte[]) payload));
            }
            else
            {
                throw new TransformerException(MessageFactory
                    .createStaticMessage("Unable to convert " + payload.getClass() + " to XMLStreamReader."));
            }
        
            if (reversible)
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

}


