/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.transformers;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.util.IOUtils;

import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;

import org.w3c.dom.Document;

import de.odysseus.staxon.json.JsonXMLOutputFactory;

/**
 * Convert XML to a JSON string
 */
public class XmlToJson extends AbstractToFromXmlTransformer {

  public XmlToJson() {
    this.registerSourceType(DataType.STRING);
    this.registerSourceType(DataType.INPUT_STREAM);
    this.registerSourceType(DataType.BYTE_ARRAY);
    this.registerSourceType(DataType.fromType(Reader.class));
    this.registerSourceType(DataType.fromType(URL.class));
    this.registerSourceType(DataType.fromType(File.class));
    registerSourceType(DataType.fromType(Document.class));
    this.setReturnDataType(DataType.JSON_STRING);
  }

  /**
   * Use Staxon to convert XML to a JSON string
   */
  @Override
  protected Object doTransform(Object src, Charset enc) throws TransformerException {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);

    TransformerInputs inputs = null;
    Source source;
    try {
      if (src instanceof Document) {
        Document doc = (Document) src;
        String location = doc.getDocumentURI();
        if (location == null) {
          location = "(Document)";
        }
        source = new DOMSource(doc, location);
      } else {
        inputs = new TransformerInputs(this, src);
        if (inputs.getInputStream() != null) {
          source = new StAXSource(inputFactory.createXMLStreamReader(inputs.getInputStream(),
                                                                     enc == null ? UTF_8.name() : enc.name()));
        } else {
          source = new StAXSource(inputFactory.createXMLStreamReader(inputs.getReader()));
        }
      }
      XMLOutputFactory outputFactory = new JsonXMLOutputFactory();
      outputFactory.setProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY, true);
      outputFactory.setProperty(JsonXMLOutputFactory.PROP_PRETTY_PRINT, true);
      return convert(source, outputFactory);
    } catch (Exception ex) {
      throw new TransformerException(this, ex);
    } finally {
      if (inputs != null) {
        IOUtils.closeQuietly(inputs.getInputStream());
        IOUtils.closeQuietly(inputs.getReader());
      }
    }
  }
}
