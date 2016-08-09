/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.module.xml.util.XMLUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
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
import org.dom4j.Node;
import org.dom4j.io.DocumentResult;

/**
 * <code>AbstractXmlTransformer</code> offers some XSLT transform on a DOM (or other XML-ish) object.
 */
public abstract class AbstractXmlTransformer extends AbstractMessageTransformer implements Initialisable {

  private String outputEncoding;
  private XMLInputFactory xmlInputFactory;
  private XMLOutputFactory xmlOutputFactory;
  private boolean useStaxSource = false;
  private boolean acceptExternalEntities = false;

  public AbstractXmlTransformer() {
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.fromType(javax.xml.transform.Source.class));
    registerSourceType(DataType.fromType(org.xml.sax.InputSource.class));
    registerSourceType(DataType.fromType(org.dom4j.Node.class));
    registerSourceType(DataType.fromType(org.dom4j.Document.class));
    registerSourceType(DataType.fromType(org.w3c.dom.Document.class));
    registerSourceType(DataType.fromType(org.w3c.dom.Element.class));
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.fromType(OutputHandler.class));
    registerSourceType(DataType.fromType(javax.xml.stream.XMLStreamReader.class));
    registerSourceType(DataType.fromType(org.mule.runtime.module.xml.transformer.DelayedResult.class));
    setReturnDataType(DataType.builder().type(byte[].class).mediaType(MediaType.XML).build());
  }

  @Override
  public final void initialise() throws InitialisationException {
    xmlInputFactory = XMLInputFactory.newInstance();

    if (!acceptExternalEntities) {
      xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      useStaxSource = true;
    }

    xmlOutputFactory = XMLOutputFactory.newInstance();

    this.doInitialise();
  }

  protected void doInitialise() throws InitialisationException {
    // template method
  }

  /** Result callback interface used when processing XML through JAXP */
  protected static interface ResultHolder {

    /**
     * @return A Result to use in a transformation (e.g. writing a DOM to a stream)
     */
    Result getResult();

    /** @return The actual result as produced after the call to 'transform'. */
    Object getResultObject();
  }

  /**
   * @param desiredClass Java class representing the desired format
   * @return Callback interface representing the desiredClass - or null if the return class isn't supported (or is null).
   */
  protected static ResultHolder getResultHolder(Class<?> desiredClass) {
    if (desiredClass == null) {
      return null;
    }
    if (byte[].class.equals(desiredClass) || InputStream.class.isAssignableFrom(desiredClass)) {
      return new ResultHolder() {

        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(resultStream);

        @Override
        public Result getResult() {
          return result;
        }

        @Override
        public Object getResultObject() {
          return resultStream.toByteArray();
        }
      };
    } else if (String.class.equals(desiredClass)) {
      return new ResultHolder() {

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        @Override
        public Result getResult() {
          return result;
        }

        @Override
        public Object getResultObject() {
          return writer.getBuffer().toString();
        }
      };
    } else if (org.w3c.dom.Document.class.isAssignableFrom(desiredClass)) {
      final DOMResult result;

      try {
        result = new DOMResult(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
      } catch (Exception e) {
        throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not create result document"), e);
      }

      return new ResultHolder() {

        @Override
        public Result getResult() {
          return result;
        }

        @Override
        public Object getResultObject() {
          return result.getNode();
        }
      };
    } else if (org.dom4j.io.DocumentResult.class.isAssignableFrom(desiredClass)) {
      return new ResultHolder() {

        DocumentResult result = new DocumentResult();

        @Override
        public Result getResult() {
          return result;
        }

        @Override
        public Object getResultObject() {
          return result;
        }
      };
    } else if (org.dom4j.Document.class.isAssignableFrom(desiredClass)) {
      return new ResultHolder() {

        DocumentResult result = new DocumentResult();

        @Override
        public Result getResult() {
          return result;
        }

        @Override
        public Object getResultObject() {
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
   * @throws TransformerFactoryConfigurationError On error
   * @throws javax.xml.transform.TransformerException On error
   * @throws TransformerException
   * @deprecated Replaced by convertToText(Object obj, String ouputEncoding)
   */
  @Deprecated
  protected String convertToText(Object obj) throws Exception {
    return convertToText(obj, null);
  }

  /**
   * Converts an XML in-memory representation to a String using a specific encoding. If using an encoding which cannot represent
   * specific characters, these are written as entities, even if they can be represented as a Java String.
   *
   * @param obj Object to convert (could be byte[], String, DOM, or DOM4J Document). If the object is a byte[], the character
   *        encoding used MUST match the declared encoding standard, or a parse error will occur.
   * @param outputEncoding Name of the XML encoding to use, e.g. US-ASCII, or null for UTF-8
   * @return String including XML header using the specified encoding
   * @throws TransformerFactoryConfigurationError On error
   * @throws javax.xml.transform.TransformerException On error
   * @throws TransformerException
   */
  protected String convertToText(Object obj, Charset outputEncoding) throws Exception {
    // Catch the direct translations
    if (obj instanceof String) {
      return (String) obj;
    } else if (obj instanceof Node) {
      return ((Node) obj).asXML();
    }
    // No easy fix, so use the transformer.
    Source src = XMLUtils.toXmlSource(xmlInputFactory, useStaxSource, obj);
    if (src == null) {
      return null;
    }

    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);

    Transformer idTransformer = TransformerFactory.newInstance().newTransformer();
    if (outputEncoding != null) {
      idTransformer.setOutputProperty(OutputKeys.ENCODING, outputEncoding.name());
    }
    idTransformer.transform(src, result);
    return writer.getBuffer().toString();
  }

  /**
   * Converts an XML in-memory representation to a String using a specific encoding.
   *
   * @param obj Object to convert (could be byte[], String, DOM, or DOM4J Document). If the object is a byte[], the character
   *        encoding used MUST match the declared encoding standard, or a parse error will occur.
   * @param outputEncoding Name of the XML encoding to use, e.g. US-ASCII, or null for UTF-8
   * @return String including XML header using the specified encoding
   * @throws TransformerFactoryConfigurationError On error
   * @throws javax.xml.transform.TransformerException On error
   * @throws TransformerException
   */
  protected String convertToBytes(Object obj, Charset outputEncoding) throws Exception {
    // Always use the transformer, even for byte[] (to get the encoding right!)
    Source src = XMLUtils.toXmlSource(xmlInputFactory, useStaxSource, obj);
    if (src == null) {
      return null;
    }

    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);

    Transformer idTransformer = XMLUtils.getTransformer();
    idTransformer.setOutputProperty(OutputKeys.ENCODING, outputEncoding.name());
    idTransformer.transform(src, result);
    return writer.getBuffer().toString();
  }

  protected void writeToStream(Object obj, Charset outputEncoding, OutputStream output) throws Exception {
    // Always use the transformer, even for byte[] (to get the encoding right!)
    Source src = XMLUtils.toXmlSource(xmlInputFactory, useStaxSource, obj);
    if (src == null) {
      return;
    }

    StreamResult result = new StreamResult(output);

    Transformer idTransformer = XMLUtils.getTransformer();
    idTransformer.setOutputProperty(OutputKeys.ENCODING, outputEncoding.name());
    idTransformer.transform(src, result);
  }

  /** @return the outputEncoding */
  public String getOutputEncoding() {
    return outputEncoding;
  }

  /** @param outputEncoding the outputEncoding to set */
  public void setOutputEncoding(String outputEncoding) {
    this.outputEncoding = outputEncoding;
  }

  public boolean isUseStaxSource() {
    return useStaxSource;
  }

  public void setUseStaxSource(boolean useStaxSource) {
    this.useStaxSource = useStaxSource;
  }

  public XMLInputFactory getXMLInputFactory() {
    return xmlInputFactory;
  }

  public void setXMLInputFactory(XMLInputFactory xmlInputFactory) {
    this.xmlInputFactory = xmlInputFactory;
  }

  public XMLOutputFactory getXMLOutputFactory() {
    return xmlOutputFactory;
  }

  public void setXMLOutputFactory(XMLOutputFactory xmlOutputFactory) {
    this.xmlOutputFactory = xmlOutputFactory;
  }

  public void setAcceptExternalEntities(boolean acceptExternalEntities) {
    this.acceptExternalEntities = acceptExternalEntities;
  }

  public boolean getAcceptExternalEntities() {
    return this.acceptExternalEntities;
  }
}
