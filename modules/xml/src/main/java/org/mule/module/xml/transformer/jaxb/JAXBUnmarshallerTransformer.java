/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer.jaxb;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.w3c.dom.Node;

/**
 * TODO
 */
public class JAXBUnmarshallerTransformer extends AbstractTransformer
{
    protected JAXBContext jaxbContext;

    public JAXBUnmarshallerTransformer()
    {
        registerSourceType(String.class);
        registerSourceType(Writer.class);
        registerSourceType(File.class);
        registerSourceType(URL.class);
        registerSourceType(Node.class);
        registerSourceType(InputStream.class);
        registerSourceType(Source.class);
        registerSourceType(XMLStreamReader.class);
        registerSourceType(XMLEventReader.class);
    }

    public JAXBUnmarshallerTransformer(JAXBContext jaxbContext, DataType returnType)
    {
        this();
        this.jaxbContext = jaxbContext;
        setReturnDataType(returnType);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (jaxbContext == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("jaxbContext"), this);
        }
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            final Unmarshaller u = jaxbContext.createUnmarshaller();
            if (src instanceof String)
            {
                return u.unmarshal(new StringReader((String) src));
            }
            else if (src instanceof File)
            {
                return u.unmarshal((File) src);
            }
            else if (src instanceof URL)
            {
                return u.unmarshal((URL) src);
            }
            else if (src instanceof InputStream)
            {
                return u.unmarshal((InputStream) src);
            }
            else if (src instanceof Node)
            {
                JAXBElement e = u.unmarshal((Node) src, getReturnClass());
                return e.getValue();
            }
            else if (src instanceof Source)
            {
                JAXBElement e = u.unmarshal((Source) src, getReturnClass());
                return e.getValue();
            }
            else if (src instanceof XMLStreamReader)
            {
                JAXBElement e = u.unmarshal((XMLStreamReader) src, getReturnClass());
                return e.getValue();
            }
            else if (src instanceof XMLEventReader)
            {
                JAXBElement e = u.unmarshal((XMLEventReader) src, getReturnClass());
                return e.getValue();
            }

        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
        return null;
    }

    public JAXBContext getJaxbContext()
    {
        return jaxbContext;
    }

    public void setJaxbContext(JAXBContext jaxbContext)
    {
        this.jaxbContext = jaxbContext;
    }
}
