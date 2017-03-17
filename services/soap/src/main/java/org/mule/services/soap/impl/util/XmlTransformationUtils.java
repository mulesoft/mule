/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.util;

import org.mule.runtime.core.util.xmlsecurity.XMLSecureFactories;
import org.mule.services.soap.api.SoapService;
import org.mule.services.soap.impl.xml.stax.StaxSource;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * {@link SoapService} Transformation utility class
 *
 * @since 4.0
 */
public class XmlTransformationUtils {

  public static Document xmlStreamReaderToDocument(XMLStreamReader xmlStreamReader) throws XmlTransformationException {
    StaxSource staxSource = new StaxSource(xmlStreamReader);
    DOMResult writer = new DOMResult();
    TransformerFactory idTransformer = new SaxonTransformerFactory();
    try {
      Transformer transformer = idTransformer.newTransformer();
      transformer.transform(staxSource, writer);
    } catch (TransformerException e) {
      throw new XmlTransformationException("Error transforming XML Stream Reader to String", e);
    }
    return (Document) writer.getNode();
  }

  public static Element stringToDomElement(String xml) throws XmlTransformationException {
    try {
      DocumentBuilder db = XMLSecureFactories.createDefault().getDocumentBuilderFactory().newDocumentBuilder();
      InputSource is = new InputSource();
      is.setCharacterStream(new StringReader(xml));
      return db.parse(is).getDocumentElement();
    } catch (Exception e) {
      throw new XmlTransformationException("Could not transform xml string to Dom Element", e);
    }
  }

  public static Document stringToDocument(String xml) throws XmlTransformationException {
    DocumentBuilderFactory factory = XMLSecureFactories.createDefault().getDocumentBuilderFactory();
    try {
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(new ByteArrayInputStream(xml.getBytes()));
    } catch (Exception e) {
      throw new XmlTransformationException("Could not transform xml to Dom Document", e);
    }
  }

  public static String nodeToString(Node node) throws XmlTransformationException {
    try {
      StringWriter writer = new StringWriter();
      DOMSource source = new DOMSource(node);
      StreamResult result = new StreamResult(writer);
      TransformerFactory idTransformer = new SaxonTransformerFactory();
      Transformer transformer = idTransformer.newTransformer();
      transformer.transform(source, result);
      return writer.toString();
    } catch (Exception e) {
      throw new XmlTransformationException("Could not transform Node to String", e);
    }
  }

  public static XMLStreamReader stringToXmlStreamReader(String xml) throws XmlTransformationException {
    try {
      return XMLSecureFactories.createDefault().getXMLInputFactory()
          .createXMLStreamReader(new ByteArrayInputStream(xml.getBytes()));
    } catch (Exception e) {
      throw new XmlTransformationException("Could not transform xml to XmlStreamReader", e);
    }
  }
}
