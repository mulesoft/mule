/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import static com.ctc.wstx.api.WstxInputProperties.P_MAX_ATTRIBUTE_SIZE;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.mule.api.config.MuleProperties.MULE_MAX_ATTRIBUTE_SIZE;
import static org.mule.util.xmlsecurity.XMLSecureFactories.createWithConfig;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;
import org.mule.module.xml.stax.DelegateXMLStreamReader;
import org.mule.module.xml.stax.StaxSource;
import org.mule.module.xml.transformer.DelayedResult;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.xmlsecurity.XMLSecureFactories;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.DocumentSource;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * General utility methods for working with XML.
 */
public class XMLUtils extends org.mule.util.XMLUtils
{
    public static final String XPATH1_FALLBACK = "mule.xml.xpath10.fallback";

    // xml parser feature names for optional XSD validation
    public static final String APACHE_XML_FEATURES_VALIDATION_SCHEMA = "http://apache.org/xml/features/validation/schema";
    public static final String APACHE_XML_FEATURES_VALIDATION_SCHEMA_FULL_CHECKING = "http://apache.org/xml/features/validation/schema-full-checking";

    // JAXP property for specifying external XSD location
    public static final String JAXP_PROPERTIES_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    // JAXP properties for specifying external XSD language (as required by newer
    // JAXP implementation)
    public static final String JAXP_PROPERTIES_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String JAXP_PROPERTIES_SCHEMA_LANGUAGE_VALUE = "http://www.w3.org/2001/XMLSchema";

    // Shipped with Mule
    public static final String SAXON_TRANSFORMER_FACTORY = "net.sf.saxon.TransformerFactoryImpl";
    public static final String WSTX_INPUT_FACTORY = "com.ctc.wstx.stax.WstxInputFactory";

    private static final Logger LOGGER = getLogger(XMLUtils.class);

    /**
     * Converts a DOM to an XML string.
     * @param dom the dome object to convert
     * @return A string representation of the document
     */
    public static String toXml(Document dom)
    {
        return new DOMReader().read(dom).asXML();
    }

    /**
     * @return a new XSLT transformer
     * @throws TransformerConfigurationException if no TransformerFactory can be located in the
     * runtime environment.
     */
    public static Transformer getTransformer() throws TransformerConfigurationException
    {
        return XMLSecureFactories.createDefault().getTransformerFactory().newTransformer();
    }

    public static org.dom4j.Document toDocument(Object obj, MuleContext muleContext) throws Exception
    {
        return toDocument(obj, null, muleContext);
    }
    
    /**
     * Converts an object of unknown type to an org.dom4j.Document if possible.
     * @return null if object cannot be converted
     * @throws DocumentException if an error occurs while parsing
     */
    public static org.dom4j.Document toDocument(Object obj, String externalSchemaLocation, MuleContext muleContext) throws Exception
    {
        org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
        if (externalSchemaLocation != null)
        {
            reader.setValidation(true);
            reader.setFeature(APACHE_XML_FEATURES_VALIDATION_SCHEMA, true);
            reader.setFeature(APACHE_XML_FEATURES_VALIDATION_SCHEMA_FULL_CHECKING, true);
            
            InputStream xsdAsStream = IOUtils.getResourceAsStream(externalSchemaLocation, XMLUtils.class);
            if (xsdAsStream == null)
            {
                throw new IllegalArgumentException("Couldn't find schema at " + externalSchemaLocation);
            }
    
            // Set schema language property (must be done before the schemaSource
            // is set)
            reader.setProperty(JAXP_PROPERTIES_SCHEMA_LANGUAGE, JAXP_PROPERTIES_SCHEMA_LANGUAGE_VALUE);
    
            // Need this one to map schemaLocation to a physical location
            reader.setProperty(JAXP_PROPERTIES_SCHEMA_SOURCE, xsdAsStream);
        }


        if (obj instanceof org.dom4j.Document)
        {
            return (org.dom4j.Document) obj;
        }
        else if (obj instanceof org.w3c.dom.Document)
        {
            org.dom4j.io.DOMReader domReader = new org.dom4j.io.DOMReader();
            return domReader.read((org.w3c.dom.Document) obj);
        }
        else if (obj instanceof org.xml.sax.InputSource)
        {                
            return reader.read((org.xml.sax.InputSource) obj);
        }
        else if (obj instanceof javax.xml.transform.Source || obj instanceof XMLStreamReader)
        {                
            // TODO Find a more direct way to do this
            XmlToDomDocument tr = new XmlToDomDocument();
            tr.setMuleContext(muleContext);
            tr.setReturnDataType(DataTypeFactory.create(org.dom4j.Document.class));
            return (org.dom4j.Document) tr.transform(obj);
        }
        else if (obj instanceof java.io.InputStream)
        {                
            return reader.read((java.io.InputStream) obj);
        }
        else if (obj instanceof String)
        {
            return reader.read(new StringReader((String) obj));
        }
        else if (obj instanceof byte[])
        {
            // TODO Handle encoding/charset somehow
            return reader.read(new StringReader(new String((byte[]) obj)));
        }
        else if (obj instanceof File)
        {                
            return reader.read((File) obj);
        }
        else
        {
            return null;
        }
    }

    /**
     * Converts a payload to a {@link org.w3c.dom.Document} representation.
     * <p> Reproduces the behavior from {@link org.mule.module.xml.util.XMLUtils#toDocument(Object, MuleContext)}
     * which works converting to {@link org.dom4j.Document}.
     *
     * @param payload the payload to convert.
     * @return a document from the payload or null if the payload is not a valid XML document.
     */
    public static org.w3c.dom.Document toW3cDocument(Object payload) throws Exception
    {
        if (payload instanceof org.dom4j.Document)
        {
            DOMWriter writer = new DOMWriter();
            org.w3c.dom.Document w3cDocument = writer.write((org.dom4j.Document) payload);

            return w3cDocument;
        }
        else if (payload instanceof org.w3c.dom.Document)
        {
            return (org.w3c.dom.Document) payload;
        }
        else if (payload instanceof org.xml.sax.InputSource)
        {
            return parseXML((InputSource) payload);
        }
        else if (payload instanceof javax.xml.transform.Source || payload instanceof XMLStreamReader)
        {
            DOMResult result = new DOMResult();
            Transformer idTransformer = getTransformer();
            Source source = (payload instanceof Source) ? (Source)payload : toXmlSource(null, true, payload);
            idTransformer.transform(source, result);
            return (Document) result.getNode();
        }
        else if (payload instanceof java.io.InputStream)
        {
            InputStreamReader input = new InputStreamReader((InputStream) payload);
            return parseXML(input);
        }
        else if (payload instanceof String)
        {
            Reader input = new StringReader((String) payload);

            return parseXML(input);
        }
        else if (payload instanceof byte[])
        {
            // TODO Handle encoding/charset somehow
            Reader input = new StringReader(new String((byte[]) payload));
            return parseXML(input);
        }
        else if (payload instanceof File)
        {
            Reader input = new FileReader((File) payload);
            return parseXML(input);
        }
        else
        {
            return null;
        }
    }

    private static org.w3c.dom.Document parseXML(Reader source) throws Exception
    {
        return parseXML(new InputSource(source));
    }

    private static org.w3c.dom.Document parseXML(InputSource source) throws Exception
    {
        DocumentBuilderFactory factory = XMLSecureFactories.createDefault().getDocumentBuilderFactory();
        return factory.newDocumentBuilder().parse(source);
    }

    /**
     * Returns an XMLStreamReader for an object of unknown type if possible.
     * @return null if no XMLStreamReader can be created for the object type
     * @throws XMLStreamException
     * @deprecated As of 3.7.0, use {@link #toXMLStreamReader(XMLInputFactory, org.mule.api.MuleEvent, Object)} instead.
     */
    @Deprecated
    public static XMLStreamReader toXMLStreamReader(XMLInputFactory factory, Object obj) throws XMLStreamException
    {
        return toXMLStreamReader(factory, RequestContext.getEvent(), obj);
    }

    /**
     * Returns an XMLStreamReader for an object of unknown type if possible.
     * @return null if no XMLStreamReader can be created for the object type
     * @throws XMLStreamException
     */
    public static XMLStreamReader toXMLStreamReader(XMLInputFactory factory, MuleEvent event, Object obj) throws XMLStreamException
    {
        if (obj instanceof XMLStreamReader)
        {
            return (XMLStreamReader) obj;
        }
        else if (obj instanceof org.mule.module.xml.stax.StaxSource)
        {
            return ((org.mule.module.xml.stax.StaxSource) obj).getXMLStreamReader();
        }
        else if (obj instanceof javax.xml.transform.Source)
        {
            return factory.createXMLStreamReader((javax.xml.transform.Source) obj);
        }
        else if (obj instanceof org.xml.sax.InputSource)
        {
            return factory.createXMLStreamReader(((org.xml.sax.InputSource) obj).getByteStream());
        }
        else if (obj instanceof org.w3c.dom.Document)
        {
            // this requires Woodstox factory
            return factory.createXMLStreamReader(new javax.xml.transform.dom.DOMSource((org.w3c.dom.Document) obj));
        }
        else if (obj instanceof org.dom4j.Document)
        {
            return factory.createXMLStreamReader(new org.dom4j.io.DocumentSource((org.dom4j.Document) obj));
        }
        else if (obj instanceof java.io.InputStream)
        {
            final InputStream is = (java.io.InputStream) obj;
            
            XMLStreamReader xsr = factory.createXMLStreamReader(is);
            return new DelegateXMLStreamReader(xsr) 
            {
                @Override
                public void close() throws XMLStreamException
                {
                    super.close();
                    
                    try
                    {
                        is.close();
                    }
                    catch (IOException e)
                    {
                        throw new XMLStreamException(e);
                    }
                }
                
            };
        }
        else if (obj instanceof String)
        {
            return factory.createXMLStreamReader(new StringReader((String) obj));
        }
        else if (obj instanceof byte[])
        {
            // TODO Handle encoding/charset?
            return factory.createXMLStreamReader(new ByteArrayInputStream((byte[]) obj));
        }
        else if (obj instanceof java.io.ByteArrayOutputStream)
        {
            return factory.createXMLStreamReader(new ByteArrayInputStream(((java.io.ByteArrayOutputStream) obj).toByteArray()));
        }
        else if (obj instanceof OutputHandler)
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try
            {
                ((OutputHandler)obj).write(event, outputStream);
            }
            catch (IOException e)
            {
                throw new XMLStreamException(e);
            }
            return factory.createXMLStreamReader(new ByteArrayInputStream(outputStream.toByteArray()));
        }
        else
        {
            return null;
        }
    }

    public static javax.xml.transform.Source toXmlSource(XMLStreamReader src) throws Exception {
        // StaxSource requires that we advance to a start element/document event
        if (!src.isStartElement() &&
            src.getEventType() != XMLStreamConstants.START_DOCUMENT)
        {
            src.nextTag();
        }

        return new StaxSource(src);
    }

    /**
     * Convert our object to a Source type efficiently.
     */ 
    public static javax.xml.transform.Source toXmlSource(XMLInputFactory xmlInputFactory, boolean useStaxSource, Object src) throws Exception
    {
        if (src instanceof javax.xml.transform.Source)
        {
            return (Source) src;
        }
        else if (src instanceof byte[])
        {
            ByteArrayInputStream stream = new ByteArrayInputStream((byte[]) src);
            return toStreamSource(xmlInputFactory, useStaxSource, stream);
        }
        else if (src instanceof InputStream)
        {
            return toStreamSource(xmlInputFactory, useStaxSource, (InputStream) src);
        }
        else if (src instanceof String)
        {
            if (useStaxSource)
            {
                return new StaxSource(xmlInputFactory.createXMLStreamReader(new StringReader((String) src)));
            }
            else
            {
                return new StreamSource(new StringReader((String) src));
            }
        }
        else if (src instanceof org.dom4j.Document)
        {
            return new DocumentSource((org.dom4j.Document) src);
        }
        else if (src instanceof org.xml.sax.InputSource)
        {
            return new SAXSource((InputSource) src);
        }
        // TODO MULE-3555
        else if (src instanceof XMLStreamReader)
        {
            return toXmlSource((XMLStreamReader) src);
        }
        else if (src instanceof org.w3c.dom.Document || src instanceof org.w3c.dom.Element)
        {
            return new DOMSource((org.w3c.dom.Node) src);
        }
        else if (src instanceof DelayedResult) 
        {
            DelayedResult result = ((DelayedResult) src);
            DOMResult domResult = new DOMResult();
            result.write(domResult);
            return new DOMSource(domResult.getNode());
        }
        else if (src instanceof OutputHandler) 
        {
            OutputHandler handler = ((OutputHandler) src);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            
            handler.write(RequestContext.getEvent(), output);
            
            return toStreamSource(xmlInputFactory, useStaxSource, new ByteArrayInputStream(output.toByteArray()));
        }
        else
        {
            return null;
        }
    }

    public static javax.xml.transform.Source toStreamSource(XMLInputFactory xmlInputFactory, boolean useStaxSource, final InputStream stream) throws XMLStreamException
    {
        if (useStaxSource)
        {
            XMLStreamReader xmlStreamReader = new DelegateXMLStreamReader(xmlInputFactory.createXMLStreamReader(stream))
            {
                @Override
                public void close() throws XMLStreamException
                {
                    super.close();

                    try
                    {
                        stream.close();
                    }
                    catch (IOException e)
                    {
                        throw new XMLStreamException(e);
                    }
                }
            };

            return new org.mule.module.xml.stax.StaxSource(xmlStreamReader);
        }
        else 
        {
            return new javax.xml.transform.stream.StreamSource(stream);
        }
    }

    public static Node toDOMNode(Object src, MuleEvent event) throws Exception
    {
        DocumentBuilderFactory builderFactory = XMLSecureFactories.createDefault().getDocumentBuilderFactory();
        builderFactory.setNamespaceAware(true);

        return toDOMNode(src, event, builderFactory);
    }

    public static Node toDOMNode(Object src, MuleEvent event, DocumentBuilderFactory factory) throws Exception
    {
        if (src instanceof Node)
        {
            return (Node) src;
        }
        else if (src instanceof InputSource)
        {
            return factory.newDocumentBuilder().parse((InputSource) src);
        }
        else if (src instanceof org.dom4j.Document)
        {
            return new DOMWriter().write((org.dom4j.Document) src);
        }
        else if (src instanceof OutputHandler)
        {
            OutputHandler handler = ((OutputHandler) src);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            handler.write(event, output);
            InputStream stream = new ByteArrayInputStream(output.toByteArray());

            return factory.newDocumentBuilder().parse(stream);
        }
        else if (src instanceof byte[])
        {
            ByteArrayInputStream stream = new ByteArrayInputStream((byte[]) src);
            return factory.newDocumentBuilder().parse(stream);
        }
        else if (src instanceof InputStream)
        {
            return factory.newDocumentBuilder().parse((InputStream) src);
        }
        else if (src instanceof String)
        {
            return factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader((String) src)));
        }
        else if (src instanceof XMLStreamReader)
        {
            XMLStreamReader xsr = (XMLStreamReader) src;

            // StaxSource requires that we advance to a start element/document event
            if (!xsr.isStartElement() && xsr.getEventType() != XMLStreamConstants.START_DOCUMENT)
            {
                xsr.nextTag();
            }

            final InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(getOuterXml(xsr)));
            return factory.newDocumentBuilder().parse(is);
        }
        else if (src instanceof DelayedResult)
        {
            DelayedResult result = ((DelayedResult) src);
            DOMResult domResult = new DOMResult();
            result.write(domResult);
            return domResult.getNode();
        }
        else
        {
            return null;
        }
    }

    private static String getOuterXml(XMLStreamReader xmlr) throws TransformerFactoryConfigurationError, TransformerException
    {
        Transformer transformer = XMLSecureFactories.createDefault().getTransformerFactory().newTransformer();
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new StaxSource(xmlr), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Copies the reader to the writer. The start and end document methods must
     * be handled on the writer manually. TODO: if the namespace on the reader
     * has been declared previously to where we are in the stream, this probably
     * won't work.
     * 
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        copy(reader, writer, false);
    }
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer,
                            boolean fragment) throws XMLStreamException {
        // number of elements read in
        int read = 0;
        int event = reader.getEventType();

        while (reader.hasNext()) {
            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                read++;
                writeStartElement(reader, writer);
                break;
            case XMLStreamConstants.END_ELEMENT:
                writer.writeEndElement();
                read--;
                if (read <= 0 && !fragment) {
                    return;
                }
                break;
            case XMLStreamConstants.CHARACTERS:
                writer.writeCharacters(reader.getText());
                break;
            case XMLStreamConstants.START_DOCUMENT:
            case XMLStreamConstants.END_DOCUMENT:
            case XMLStreamConstants.ATTRIBUTE:
            case XMLStreamConstants.NAMESPACE:
                break;
            default:
                break;
            }
            event = reader.next();
        }
    }

    private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer)
        throws XMLStreamException {
        String local = reader.getLocalName();
        String uri = reader.getNamespaceURI();
        String prefix = reader.getPrefix();
        if (prefix == null) {
            prefix = "";
        }

        
//        System.out.println("STAXUTILS:writeStartElement : node name : " + local +  " namespace URI" + uri);
        boolean writeElementNS = false;
        if (uri != null) {
            String boundPrefix = writer.getPrefix(uri);
            if (boundPrefix == null || !prefix.equals(boundPrefix)) {
                writeElementNS = true;
            }
        }

        // Write out the element name
        if (uri != null) {
            if (prefix.length() == 0 && StringUtils.isEmpty(uri)) {
                writer.writeStartElement(local);
                writer.setDefaultNamespace(uri);

            } else {
                writer.writeStartElement(prefix, local, uri);
                writer.setPrefix(prefix, uri);
            }
        } else {
            writer.writeStartElement(local);
        }

        // Write out the namespaces
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String nsURI = reader.getNamespaceURI(i);
            String nsPrefix = reader.getNamespacePrefix(i);
            if (nsPrefix == null) {
                nsPrefix = "";
            }

            if (nsPrefix.length() == 0) {
                writer.writeDefaultNamespace(nsURI);
            } else {
                writer.writeNamespace(nsPrefix, nsURI);
            }

            if (nsURI.equals(uri) && nsPrefix.equals(prefix)) {
                writeElementNS = false;
            }
        }

        // Check if the namespace still needs to be written.
        // We need this check because namespace writing works
        // different on Woodstox and the RI.
        if (writeElementNS) {
            if (prefix.length() == 0) {
                writer.writeDefaultNamespace(uri);
            } else {
                writer.writeNamespace(prefix, uri);
            }
        }        
        
        // Write out attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String ns = reader.getAttributeNamespace(i);
            String nsPrefix = reader.getAttributePrefix(i);
            if (ns == null || ns.length() == 0) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else if (nsPrefix == null || nsPrefix.length() == 0) {
                writer.writeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                                      reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(reader.getAttributePrefix(i), reader.getAttributeNamespace(i), reader
                    .getAttributeLocalName(i), reader.getAttributeValue(i));
            }

        }
    }

    /**
     * Creates an XPath object with a custom NamespaceContext given the Node to operate on
     * @param node the Node or document to operate on.  Note that namespace handling will not work if a Node fragment is passed in
     * @return a new XPath object
     */
    private static XPath createXPath(Node node)
    {
        XPath xp = XPathFactory.newInstance().newXPath();
        if (node instanceof Document)
        {
            xp.setNamespaceContext(new XPathNamespaceContext((Document) node));
        }
        return xp;
    }

    /**
     * Select a single XML node using an Xpath
     * @param xpath the XPath expression to evaluate
     * @param node the node (or document) to exaluate on
     * @return the result of the evaluation.
     * @throws XPathExpressionException if the XPath expression is malformed and cannot be parsed
     */
    public static Node selectOne(String xpath, Node node) throws XPathExpressionException
    {
            XPath xp = createXPath(node);
            return (Node) xp.evaluate(xpath, node, XPathConstants.NODE);
    }

    /**
     * Select a single XML String value using an Xpath
     * @param xpath the XPath expression to evaluate
     * @param node the node (or document) to evaluate on
     * @return the result of the evaluation.
     * @throws XPathExpressionException if the XPath expression is malformed and cannot be parsed
     */
    public static String selectValue(String xpath, Node node) throws XPathExpressionException
    {
            XPath xp = createXPath(node);
            return (String) xp.evaluate(xpath, node, XPathConstants.STRING);
    }

    /**
     * Select a set of Node objects using the Xpath expression
     * @param xpath the XPath expression to evaluate
     * @param node the node (or document) to evaluate on
     * @return the result of the evaluation. 
     * @throws XPathExpressionException if the XPath expression is malformed and cannot be parsed
     */
    public static List<Node> select(String xpath, Node node) throws XPathExpressionException
    {
            XPath xp = createXPath(node);
            NodeList nl = (NodeList) xp.evaluate(xpath, node, XPathConstants.NODESET);
            List<Node> nodeList = new ArrayList<>(nl.getLength());
            for (int i = 0; i < nl.getLength(); i++)
            {
                nodeList.add(nl.item(i));
            }
            return nodeList;
    }

    public static Object createInstance(String className)
    {
        Object factory;

        try
        {
            factory = ClassUtils.instanciateClass(className, ClassUtils.NO_ARGS, XMLUtils.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return factory;
    }

    public static TransformerFactory createSaxonTransformerFactory() {
        TransformerFactory factory = (TransformerFactory) createInstance(SAXON_TRANSFORMER_FACTORY);

        XMLSecureFactories.createDefault().configureTransformerFactory(factory);

        return factory;
    }

    public static XMLInputFactory createWstxXmlInputFactory() {
        XMLInputFactory factory = (XMLInputFactory) createInstance(WSTX_INPUT_FACTORY);

        factory.setProperty(P_MAX_ATTRIBUTE_SIZE, MAX_VALUE);

        XMLSecureFactories.createDefault().configureXMLInputFactory(factory);

        return factory;
    }

    public static XMLInputFactory createWstxXmlInputFactory(Boolean externalEntities, Boolean expandEntities) {
        XMLInputFactory factory = (XMLInputFactory) createInstance(WSTX_INPUT_FACTORY);

        setMaxAttributeSizeProperty(factory);

        createWithConfig(externalEntities, expandEntities).configureXMLInputFactory(factory);

        return factory;
    }

    private static void setMaxAttributeSizeProperty(XMLInputFactory factory) {
        String maxAttributeSizeProperty = getProperty(MULE_MAX_ATTRIBUTE_SIZE);
        if(maxAttributeSizeProperty != null)
        {
            Integer maxAttributeSize = parseInt(maxAttributeSizeProperty);
            if(maxAttributeSize > 0 && maxAttributeSize < MAX_VALUE)
            {
                factory.setProperty(P_MAX_ATTRIBUTE_SIZE, maxAttributeSizeProperty);
                return;
            }
            else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid " + P_MAX_ATTRIBUTE_SIZE + " property value");
            }
        }
        factory.setProperty(P_MAX_ATTRIBUTE_SIZE, MAX_VALUE);
    }

    /**
     * The default namespace context that will read namespaces from the current document if the
     * Node being processed is a Document
     */
    private static class XPathNamespaceContext implements NamespaceContext
    {
        private Document document;

        public XPathNamespaceContext(Document document)
        {
            this.document = document;
        }

        @Override
        public String getNamespaceURI(String prefix)
        {
            if (prefix == null || prefix.equals(""))
            {
                return document.getDocumentElement().getNamespaceURI();
            }
            else
            {
                return document.lookupNamespaceURI(prefix);
            }
        }

        @Override
        public String getPrefix(String namespaceURI)
        {
            return document.lookupPrefix(namespaceURI);
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI)
        {
            List<String> list = new ArrayList<>();
            list.add(getPrefix(namespaceURI));
            return list.iterator();
        }
    }
}
