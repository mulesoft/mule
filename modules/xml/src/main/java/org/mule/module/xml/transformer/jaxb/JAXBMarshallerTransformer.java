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

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * TODO
 */
public class JAXBMarshallerTransformer extends AbstractTransformer
{
    protected JAXBContext jaxbContext;

    public JAXBMarshallerTransformer()
    {
        setReturnClass(InputStream.class);
        registerSourceType(Object.class);
    }

    public JAXBMarshallerTransformer(JAXBContext jaxbContext, DataType returnType)
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
            final Marshaller m = jaxbContext.createMarshaller();
            if (getReturnClass().equals(String.class))
            {
                Writer w = new StringWriter();
                m.marshal(src, w);
                return w.toString();
            }
            else if (getReturnClass().isAssignableFrom(Writer.class))
            {
                Writer w = new StringWriter();
                m.marshal(src, w);
                return w;
            }
            else if (Document.class.isAssignableFrom(getReturnClass()))
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().newDocument();
                m.marshal(src, doc);
                return doc;
            }
            else if (OutputStream.class.isAssignableFrom(getReturnClass()))
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                m.marshal(src, out);
                return out;
            }
            else if (OutputHandler.class.equals(getReturnClass()))
            {
                return new OutputHandler()
                {
                    public void write(MuleEvent event, OutputStream out) throws IOException
                    {
                        try
                        {
                            m.marshal(event.getMessage().getPayload(), out);
                        }
                        catch (JAXBException e)
                        {
                            IOException iox = new IOException("failed to mashal objec tto XML");
                            iox.initCause(e);
                            throw iox;
                        }
                    }
                };
            }

        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
        return null;
    }

    public JAXBContext getJAXBContext()
    {
        return jaxbContext;
    }

    public void setJAXBContext(JAXBContext context)
    {
        this.jaxbContext = context;
    }
}
