/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.stax;

import javanet.staxutils.StAXReaderToContentHandler;
import javanet.staxutils.StAXSource;
import javanet.staxutils.helpers.XMLFilterImplEx;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * A StaxSource which gives us access to the underlying XMLStreamReader if we are
 * StaxCapable down the line.
 */
public class StaxSource extends StAXSource
{

    private XMLStreamReader reader;

    // StAX to SAX converter that will read from StAX and produce SAX
    // this object will be wrapped by the XMLReader exposed to the client
    protected final StAXReaderToContentHandler handler;

    // SAX allows ContentHandler to be changed during the parsing,
    // but JAXB doesn't. So this repeater will sit between those
    // two components.
    protected XMLFilterImplEx repeater = new XMLFilterImplEx();

    protected final XMLReader pseudoParser = new PseudoReader();

    public StaxSource(XMLStreamReader reader)
    {
        super(reader);

        this.reader = reader;

        this.handler = new XMLStreamReaderToContentHandler(reader, repeater);

        super.setXMLReader(pseudoParser);
        // pass a dummy InputSource. We don't care
        super.setInputSource(new InputSource());
    }

    public XMLStreamReader getXMLStreamReader()
    {
        return reader;
    }

    public final class PseudoReader implements XMLReader
    {
        // we will store this value but never use it by ourselves.
        private EntityResolver entityResolver;
        private DTDHandler dtdHandler;
        private ErrorHandler errorHandler;

        public boolean getFeature(String name) throws SAXNotRecognizedException
        {
            if ("http://xml.org/sax/features/namespaces".equals(name))
            {
                return true;
            }
            else if ("http://xml.org/sax/features/namespace-prefixes".equals(name))
            {
                return repeater.getNamespacePrefixes();
            }
            else if ("http://xml.org/sax/features/external-general-entities".equals(name))
            {
                return true;
            }
            else if ("http://xml.org/sax/features/external-parameter-entities".equals(name))
            {
                return true;
            }

            throw new SAXNotRecognizedException(name);
        }

        public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException
        {
            if ("http://xml.org/sax/features/namespaces".equals(name))
            {
                // Presently we only support namespaces==true. [Issue 9]
                if (!value)
                {
                    throw new SAXNotSupportedException(name);
                }
            }
            else if ("http://xml.org/sax/features/namespace-prefixes".equals(name))
            {
                repeater.setNamespacePrefixes(value);
            }
            else if ("http://xml.org/sax/features/external-general-entities".equals(name))
            {
                // Pass over, XOM likes to get this feature
            }
            else if ("http://xml.org/sax/features/external-parameter-entities".equals(name))
            {
                // Pass over, XOM likes to get this feature
            }
            else
            {
                throw new SAXNotRecognizedException(name);
            }
        }

        public Object getProperty(String name) throws SAXNotRecognizedException
        {
            if ("http://xml.org/sax/properties/lexical-handler".equals(name))
            {
                return repeater.getLexicalHandler();
            }

            throw new SAXNotRecognizedException(name);
        }

        public void setProperty(String name, Object value) throws SAXNotRecognizedException
        {
            if ("http://xml.org/sax/properties/lexical-handler".equals(name))
            {
                repeater.setLexicalHandler((LexicalHandler) value);
            }
            else
            {
                throw new SAXNotRecognizedException(name);
            }
        }

        public void setEntityResolver(EntityResolver resolver)
        {
            this.entityResolver = resolver;
        }

        public EntityResolver getEntityResolver()
        {
            return entityResolver;
        }

        public void setDTDHandler(DTDHandler handler)
        {
            this.dtdHandler = handler;
        }

        public DTDHandler getDTDHandler()
        {
            return dtdHandler;
        }

        public void setContentHandler(ContentHandler handler)
        {
            repeater.setContentHandler(handler);
        }

        public ContentHandler getContentHandler()
        {
            return repeater.getContentHandler();
        }

        public void setErrorHandler(ErrorHandler handler)
        {
            this.errorHandler = handler;
        }

        public ErrorHandler getErrorHandler()
        {
            return errorHandler;
        }

        public void parse(InputSource input) throws SAXException
        {
            parse();
        }

        public void parse(String systemId) throws SAXException
        {
            parse();
        }

        public void parse() throws SAXException
        {
            // parses from a StAX reader and generates SAX events which
            // go through the repeater and are forwarded to the appropriate
            // component
            try
            {
                handler.bridge();
            }
            catch (XMLStreamException e)
            {
                // wrap it in a SAXException
                SAXParseException se = new SAXParseException(e.getMessage(), null, null, e.getLocation()
                    .getLineNumber(), e.getLocation().getColumnNumber(), e);

                // if the consumer sets an error handler, it is our responsibility
                // to notify it.
                if (errorHandler != null) errorHandler.fatalError(se);

                // this is a fatal error. Even if the error handler
                // returns, we will abort anyway.
                throw se;
            }
            finally
            {
                try
                {
                    reader.close();
                }
                catch (XMLStreamException e)
                {
                    throw new SAXException(e);
                }
            }
        }
    }

}
