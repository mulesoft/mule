/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.mule.config.spring.parsers.XmlMetadataAnnotations.METADATA_ANNOTATIONS_KEY;

import org.mule.config.spring.parsers.XmlMetadataAnnotations;
import org.mule.util.SystemUtils;

import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MutationEvent;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Alternative to Spring's default document loader that uses <b>SAX</b> instead of <b>DOM</b> to parse the bean
 * definitions.
 * <p/>
 * Additionally, the elements in the parsed elements are augmented with metadata annotations.
 * 
 * @since 3.8.0
 */
public class MuleDocumentLoader implements DocumentLoader
{

    /**
     * JAXP attribute used to configure the schema language for validation.
     */
    private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    /**
     * JAXP attribute value indicating the XSD schema language.
     */
    private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

    private static final Log logger = LogFactory.getLog(MuleDocumentLoader.class);

    /**
     * Load the {@link Document} at the supplied {@link InputSource} using the standard JAXP-configured XML parser.
     */
    @Override
    public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
                                 ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (logger.isDebugEnabled())
        {
            logger.debug("Using JAXP provider [" + factory.getClass().getName() + "]");
        }
        Document doc = factory.newDocumentBuilder().newDocument();

        XMLReader documentReader = createDocumentReader(validationMode, namespaceAware);

        SAXSource xmlSource = new SAXSource(createAnnotator(entityResolver, errorHandler, doc, documentReader), inputSource);
        createSaxToDomTransformer(validationMode).transform(xmlSource, new DOMResult(doc));
        return doc;
    }

    protected Transformer createSaxToDomTransformer(int validationMode) throws TransformerFactoryConfigurationError, TransformerConfigurationException
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        if (logger.isDebugEnabled())
        {
            logger.debug("Using JAXP transformer provider [" + transformerFactory.getClass().getName() + "]");
        }

        if (validationMode != XmlValidationModeDetector.VALIDATION_NONE)
        {
            // by default saxon disables schema validation, so we have to reenable it manually.
            if ("net.sf.saxon.TransformerFactoryImpl".equals(transformerFactory.getClass().getName()))
            {
                transformerFactory.setFeature("http://saxon.sf.net/feature/validation", true);
            }
        }

        return transformerFactory.newTransformer();
    }

    protected XMLReader createDocumentReader(int validationMode, boolean namespaceAware) throws ParserConfigurationException, SAXException
    {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(namespaceAware);

        if (validationMode != XmlValidationModeDetector.VALIDATION_NONE)
        {
            saxParserFactory.setValidating(true);
            if (validationMode == XmlValidationModeDetector.VALIDATION_XSD)
            {
                // Enforce namespace aware for XSD...
                saxParserFactory.setNamespaceAware(true);
            }
        }

        SAXParser saxParser = saxParserFactory.newSAXParser();
        if (validationMode == XmlValidationModeDetector.VALIDATION_XSD)
        {
            saxParser.setProperty(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
        }

        return saxParser.getXMLReader();
    }

    protected XmlMetadataAnnotator createAnnotator(EntityResolver entityResolver, ErrorHandler errorHandler, Document doc, XMLReader documentReader)
    {
        XmlMetadataAnnotator annotator = new XmlMetadataAnnotator(documentReader, doc);
        if (entityResolver != null)
        {
            annotator.setEntityResolver(entityResolver);
        }
        if (errorHandler != null)
        {
            annotator.setErrorHandler(errorHandler);
        }
        return annotator;
    }

    /**
     * SAX filter that builds the metadata that will annotate the built nodes.
     */
    private static class XmlMetadataAnnotator extends XMLFilterImpl
    {
        private Locator locator;
        private Stack<Element> elementStack = new Stack<>();
        private Stack<XmlMetadataAnnotations> annotationsStack = new Stack<>();

        private UserDataHandler dataHandler = new UserDataHandler()
        {
            /**
             * Ensure metadata is copied to any new DOM node.
             */
            @Override
            public void handle(short operation, String key, Object data, Node src, Node dst)
            {
                if (src != null && dst != null)
                {
                    XmlMetadataAnnotations metadataAnnotations = (XmlMetadataAnnotations) src.getUserData("metadataBuilder");
                    if (metadataAnnotations != null)
                    {
                        dst.setUserData(METADATA_ANNOTATIONS_KEY, metadataAnnotations, dataHandler);
                    }
                }
            }
        };

        private XmlMetadataAnnotator(XMLReader xmlReader, Document dom)
        {
            super(xmlReader);

            // Add listener to DOM, so we know which node was added.
            EventListener modListener = new EventListener()
            {
                @Override
                public void handleEvent(Event e)
                {
                    EventTarget target = ((MutationEvent) e).getTarget();
                    elementStack.push((Element) target);
                }
            };
            ((EventTarget) dom).addEventListener("DOMNodeInserted", modListener, true);
        }

        @Override
        public void setDocumentLocator(Locator locator)
        {
            super.setDocumentLocator(locator);
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
        {
            super.startElement(uri, localName, qName, atts);

            XmlMetadataAnnotations metadataBuilder = new XmlMetadataAnnotations(locator.getLineNumber());
            metadataBuilder.appendElementStart(qName, atts);
            annotationsStack.push(metadataBuilder);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            super.characters(ch, start, length);

            annotationsStack.peek().appendElementBody(new String(ch, start, length).trim());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            super.endElement(uri, localName, qName);

            XmlMetadataAnnotations metadataAnnotations = annotationsStack.pop();
            metadataAnnotations.appendElementEnd(qName);

            if (!annotationsStack.isEmpty())
            {
                annotationsStack.peek().appendElementBody(SystemUtils.LINE_SEPARATOR + metadataAnnotations.getElementString() + SystemUtils.LINE_SEPARATOR);
            }

            Element node = elementStack.pop();
            node.setUserData(METADATA_ANNOTATIONS_KEY, metadataAnnotations, dataHandler);
        }
    }
}
