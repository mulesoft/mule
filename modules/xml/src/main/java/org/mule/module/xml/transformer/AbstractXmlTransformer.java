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
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.AbstractTransformer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;

/**
 * <code>AbstractXmlTransformer</code> offers some XSLT transform on a DOM (or
 * other XML-ish) object.
 */
public abstract class AbstractXmlTransformer extends AbstractTransformer
{
    private String outputEncoding;
    private XMLInputFactory xmlInputFactory;
    private XMLOutputFactory xmlOutputFactory;
    private boolean useStaxSource = false;
    
    public AbstractXmlTransformer()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        registerSourceType(javax.xml.transform.Source.class);
        registerSourceType(org.xml.sax.InputSource.class);
        registerSourceType(org.dom4j.Document.class);
        registerSourceType(org.w3c.dom.Document.class);
        registerSourceType(org.w3c.dom.Element.class);
        registerSourceType(java.io.InputStream.class);
        registerSourceType(org.mule.api.transport.OutputHandler.class);
        registerSourceType(javax.xml.stream.XMLStreamReader.class);
        registerSourceType(org.mule.module.xml.transformer.DelayedResult.class);
        setReturnClass(byte[].class);
        
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    /** Result callback interface used when processing XML through JAXP */
    protected static interface ResultHolder
    {

        /**
         * @return A Result to use in a transformation (e.g. writing a DOM to a
         *         stream)
         */
        Result getResult();

        /** @return The actual result as produced after the call to 'transform'. */
        Object getResultObject();
    }

    /**
     * @param desiredClass Java class representing the desired format
     * @return Callback interface representing the desiredClass - or null if the
     *         return class isn't supported (or is null).
     */
    protected static ResultHolder getResultHolder(Class desiredClass)
    {
        if (desiredClass == null)
        {
            return null;
        }
        if (byte[].class.equals(desiredClass) || InputStream.class.isAssignableFrom(desiredClass))
        {
            return new ResultHolder()
            {
                ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
                StreamResult result = new StreamResult(resultStream);

                public Result getResult()
                {
                    return result;
                }

                public Object getResultObject()
                {
                    return resultStream.toByteArray();
                }
            };
        }
        else if (String.class.equals(desiredClass))
        {
            return new ResultHolder()
            {
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);

                public Result getResult()
                {
                    return result;
                }

                public Object getResultObject()
                {
                    return writer.getBuffer().toString();
                }
            };
        }
        else if (org.w3c.dom.Document.class.isAssignableFrom(desiredClass))
        {
            return new ResultHolder()
            {
                DOMResult result = new DOMResult();

                public Result getResult()
                {
                    return result;
                }

                public Object getResultObject()
                {
                    return result.getNode();
                }
            };
        }
        else if (org.dom4j.io.DocumentResult.class.isAssignableFrom(desiredClass))
        {
            return new ResultHolder()
            {
                DocumentResult result = new DocumentResult();

                public Result getResult()
                {
                    return result;
                }

                public Object getResultObject()
                {
                    return result;
                }
            };
        }
        else if (org.dom4j.Document.class.isAssignableFrom(desiredClass))
        {
            return new ResultHolder()
            {
                DocumentResult result = new DocumentResult();

                public Result getResult()
                {
                    return result;
                }

                public Object getResultObject()
                {
                    return result.getDocument();
                }
            };
        } 
        
        return null;
    }

    /**
     * Converts an XML in-memory representation to a String
     *
     * @param obj Object to convert (could be byte[], String, DOM, DOM4J)
     * @return String including XML header using default (UTF-8) encoding
     * @throws TransformerFactoryConfigurationError
     *          On error
     * @throws javax.xml.transform.TransformerException
     *          On error
     * @throws TransformerException 
     * @deprecated Replaced by convertToText(Object obj, String ouputEncoding)
     */
    protected String convertToText(Object obj)
            throws TransformerFactoryConfigurationError, javax.xml.transform.TransformerException, TransformerException
    {
        return convertToText(obj, null);
    }

    /**
     * Converts an XML in-memory representation to a String using a specific encoding.
     * If using an encoding which cannot represent specific characters, these are
     * written as entities, even if they can be represented as a Java String.
     *
     * @param obj            Object to convert (could be byte[], String, DOM, or DOM4J Document).
     *                       If the object is a byte[], the character
     *                       encoding used MUST match the declared encoding standard, or a parse error will occur.
     * @param outputEncoding Name of the XML encoding to use, e.g. US-ASCII, or null for UTF-8
     * @return String including XML header using the specified encoding
     * @throws TransformerFactoryConfigurationError
     *          On error
     * @throws javax.xml.transform.TransformerException
     *          On error
     * @throws TransformerException 
     */
    protected String convertToText(Object obj, String outputEncoding)
            throws TransformerFactoryConfigurationError, javax.xml.transform.TransformerException, TransformerException
    {
        // Catch the direct translations
        if (obj instanceof String)
        {
            return (String) obj;
        }
        else if (obj instanceof Document)
        {
            return ((Document) obj).asXML();
        }
        // No easy fix, so use the transformer.
        Source src;
        try
        {
            src = XMLUtils.toXmlSource(xmlInputFactory, useStaxSource, obj);
            if (src == null)
            {
                return null;
            }
        }
        catch (Exception e)
        {
            if (e instanceof TransformerException)
            {
                throw (TransformerException) e;
            }
            
            throw new TransformerException(this, e);
        }

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        Transformer idTransformer = TransformerFactory.newInstance().newTransformer();
        if (outputEncoding != null)
        {
            idTransformer.setOutputProperty(OutputKeys.ENCODING, outputEncoding);
        }
        idTransformer.transform(src, result);
        return writer.getBuffer().toString();
    }

    /**
     * Converts an XML in-memory representation to a String using a specific encoding.
     *
     * @param obj            Object to convert (could be byte[], String, DOM, or DOM4J Document).
     *                       If the object is a byte[], the character
     *                       encoding used MUST match the declared encoding standard, or a parse error will occur.
     * @param outputEncoding Name of the XML encoding to use, e.g. US-ASCII, or null for UTF-8
     * @return String including XML header using the specified encoding
     * @throws TransformerFactoryConfigurationError
     *          On error
     * @throws javax.xml.transform.TransformerException
     *          On error
     * @throws TransformerException 
     */
    protected String convertToBytes(Object obj, String outputEncoding)
            throws TransformerFactoryConfigurationError, javax.xml.transform.TransformerException, TransformerException
    {
        // Always use the transformer, even for byte[] (to get the encoding right!)
        Source src;
        try
        {
            src = XMLUtils.toXmlSource(xmlInputFactory, useStaxSource, obj);
            if (src == null)
            {
                return null;
            }
        }
        catch (Exception e)
        {
            if (e instanceof TransformerException)
            {
                throw (TransformerException) e;
            }
            
            throw new TransformerException(this, e);
        }

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        Transformer idTransformer = XMLUtils.getTransformer();
        idTransformer.setOutputProperty(OutputKeys.ENCODING, outputEncoding);
        idTransformer.transform(src, result);
        return writer.getBuffer().toString();
    }
    
    protected void writeToStream(Object obj, String outputEncoding, OutputStream output)
        throws TransformerFactoryConfigurationError, javax.xml.transform.TransformerException,
        TransformerException
    {
        // Always use the transformer, even for byte[] (to get the encoding right!)
        Source src;
        try
        {
            src = XMLUtils.toXmlSource(xmlInputFactory, useStaxSource, obj);
            if (src == null)
            {
                return;
            }
        }
        catch (Exception e)
        {
            if (e instanceof TransformerException)
            {
                throw (TransformerException) e;
            }
            
            throw new TransformerException(this, e);
        }

        StreamResult result = new StreamResult(output);

        Transformer idTransformer = XMLUtils.getTransformer();
        idTransformer.setOutputProperty(OutputKeys.ENCODING, outputEncoding);
        idTransformer.transform(src, result);
    }
    
    /** @return the outputEncoding */
    public String getOutputEncoding()
    {
        return outputEncoding;
    }

    /** @param outputEncoding the outputEncoding to set */
    public void setOutputEncoding(String outputEncoding)
    {
        this.outputEncoding = outputEncoding;
    }
    
    public boolean isUseStaxSource()
    {
        return useStaxSource;
    }

    public void setUseStaxSource(boolean useStaxSource)
    {
        this.useStaxSource = useStaxSource;
    }

    public XMLInputFactory getXMLInputFactory()
    {
        return xmlInputFactory;
    }

    public void setXMLInputFactory(XMLInputFactory xmlInputFactory)
    {
        this.xmlInputFactory = xmlInputFactory;
    }

    public XMLOutputFactory getXMLOutputFactory()
    {
        return xmlOutputFactory;
    }

    public void setXMLOutputFactory(XMLOutputFactory xmlOutputFactory)
    {
        this.xmlOutputFactory = xmlOutputFactory;
    }
    
}
