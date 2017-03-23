/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.xml.util;

import static org.mule.runtime.core.api.Event.getCurrentEvent;
import org.mule.extension.ws.internal.xml.stax.StaxSource;
import org.mule.extension.ws.internal.xml.transformer.DelayedResult;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.xmlsecurity.XMLSecureFactories;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.DocumentSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * General utility methods for working with XML.
 * 
 * @since 4.0, Copied from the removed XML module.
 */
public class XMLUtils {

  /**
   * @return a new XSLT transformer
   * @throws TransformerConfigurationException if no TransformerFactory can be located in the runtime environment.
   */
  public static Transformer getTransformer() throws TransformerConfigurationException {
    TransformerFactory tf = TransformerFactory.newInstance();
    if (tf != null) {
      return tf.newTransformer();
    } else {
      throw new TransformerConfigurationException("Unable to instantiate a TransformerFactory");
    }
  }

  /**
   * Converts a payload to a {@link org.w3c.dom.Document} representation.
   *
   * @param payload the payload to convert.
   * @return a document from the payload or null if the payload is not a valid XML document.
   */
  public static org.w3c.dom.Document toW3cDocument(Object payload) throws Exception {
    if (payload instanceof org.dom4j.Document) {
      DOMWriter writer = new DOMWriter();
      org.w3c.dom.Document w3cDocument = writer.write((org.dom4j.Document) payload);

      return w3cDocument;
    } else if (payload instanceof org.w3c.dom.Document) {
      return (org.w3c.dom.Document) payload;
    } else if (payload instanceof org.xml.sax.InputSource) {
      return parseXML((InputSource) payload);
    } else if (payload instanceof javax.xml.transform.Source || payload instanceof javax.xml.stream.XMLStreamReader) {
      DOMResult result = new DOMResult();
      Transformer idTransformer = getTransformer();
      Source source = (payload instanceof Source) ? (Source) payload : toXmlSource(null, true, payload);
      idTransformer.transform(source, result);
      return (Document) result.getNode();
    } else if (payload instanceof CursorStreamProvider) {
      return streamToDocument(((CursorStreamProvider) payload).openCursor());
    } else if (payload instanceof java.io.InputStream) {
      return streamToDocument((InputStream) payload);
    } else if (payload instanceof String) {
      Reader input = new StringReader((String) payload);

      return parseXML(input);
    } else if (payload instanceof byte[]) {
      // TODO Handle encoding/charset somehow
      Reader input = new StringReader(new String((byte[]) payload));
      return parseXML(input);
    } else if (payload instanceof File) {
      Reader input = new FileReader((File) payload);
      return parseXML(input);
    } else {
      return null;
    }
  }

  private static Document streamToDocument(InputStream payload) throws Exception {
    InputStreamReader input = new InputStreamReader(payload);
    return parseXML(input);
  }

  private static org.w3c.dom.Document parseXML(Reader source) throws Exception {
    return parseXML(new InputSource(source));
  }

  private static org.w3c.dom.Document parseXML(InputSource source) throws Exception {
    DocumentBuilderFactory factory = XMLSecureFactories.createDefault().getDocumentBuilderFactory();
    return factory.newDocumentBuilder().parse(source);
  }

  public static javax.xml.transform.Source toXmlSource(XMLStreamReader src) throws Exception {
    // StaxSource requires that we advance to a start element/document event
    if (!src.isStartElement() &&
        src.getEventType() != XMLStreamConstants.START_DOCUMENT) {
      src.nextTag();
    }

    return new StaxSource(src);
  }

  /**
   * Convert our object to a Source type efficiently.
   */
  public static javax.xml.transform.Source toXmlSource(javax.xml.stream.XMLInputFactory xmlInputFactory, boolean useStaxSource,
                                                       Object src)
      throws Exception {
    if (src instanceof javax.xml.transform.Source) {
      return (Source) src;
    } else if (src instanceof byte[]) {
      ByteArrayInputStream stream = new ByteArrayInputStream((byte[]) src);
      return toStreamSource(xmlInputFactory, useStaxSource, stream);
    } else if (src instanceof CursorStreamProvider) {
      return toStreamSource(xmlInputFactory, useStaxSource, ((CursorStreamProvider) src).openCursor());
    } else if (src instanceof InputStream) {
      return toStreamSource(xmlInputFactory, useStaxSource, (InputStream) src);
    } else if (src instanceof String) {
      if (useStaxSource) {
        return new StaxSource(xmlInputFactory.createXMLStreamReader(new StringReader((String) src)));
      } else {
        return new StreamSource(new StringReader((String) src));
      }
    } else if (src instanceof org.dom4j.Document) {
      return new DocumentSource((org.dom4j.Document) src);
    } else if (src instanceof org.xml.sax.InputSource) {
      return new SAXSource((InputSource) src);
    }
    // TODO MULE-3555
    else if (src instanceof XMLStreamReader) {
      return toXmlSource((XMLStreamReader) src);
    } else if (src instanceof org.w3c.dom.Document || src instanceof org.w3c.dom.Element) {
      return new DOMSource((org.w3c.dom.Node) src);
    } else if (src instanceof DelayedResult) {
      DelayedResult result = ((DelayedResult) src);
      DOMResult domResult = new DOMResult();
      result.write(domResult);
      return new DOMSource(domResult.getNode());
    } else if (src instanceof OutputHandler) {
      OutputHandler handler = ((OutputHandler) src);
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      handler.write(getCurrentEvent(), output);

      return toStreamSource(xmlInputFactory, useStaxSource, new ByteArrayInputStream(output.toByteArray()));
    } else {
      return null;
    }
  }

  public static javax.xml.transform.Source toStreamSource(javax.xml.stream.XMLInputFactory xmlInputFactory, boolean useStaxSource,
                                                          InputStream stream)
      throws XMLStreamException {
    if (useStaxSource) {
      return new org.mule.extension.ws.internal.xml.stax.StaxSource(xmlInputFactory.createXMLStreamReader(stream));
    } else {
      return new javax.xml.transform.stream.StreamSource(stream);
    }
  }

  public static String nodeToString(Node node) throws TransformerException {
    StringWriter writer = new StringWriter();
    DOMSource source = new DOMSource(node);
    StreamResult result = new StreamResult(writer);
    TransformerFactory idTransformer = new SaxonTransformerFactory();
    Transformer transformer = idTransformer.newTransformer();
    transformer.transform(source, result);
    return writer.toString();
  }
}
