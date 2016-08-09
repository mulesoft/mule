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
import javax.xml.transform.stax.StAXSource;

import de.odysseus.staxon.json.JsonXMLInputFactory;

/**
 * Convert JSON to an XML document string
 */
public class JsonToXml extends AbstractToFromXmlTransformer {

  public JsonToXml() {
    this.registerSourceType(DataType.STRING);
    this.registerSourceType(DataType.INPUT_STREAM);
    this.registerSourceType(DataType.BYTE_ARRAY);
    this.registerSourceType(DataType.fromType(Reader.class));
    this.registerSourceType(DataType.fromType(URL.class));
    this.registerSourceType(DataType.fromType(File.class));
    setReturnDataType(DataType.XML_STRING);
  }


  /**
   * Use Staxon to convert JSON to an XML string
   */
  @Override
  protected Object doTransform(Object src, Charset enc) throws TransformerException {
    XMLInputFactory inputFactory = new JsonXMLInputFactory();
    inputFactory.setProperty(JsonXMLInputFactory.PROP_MULTIPLE_PI, false);
    TransformerInputs inputs = new TransformerInputs(this, src);
    Source source;
    try {
      if (inputs.getInputStream() != null) {
        source =
            new StAXSource(inputFactory.createXMLStreamReader(inputs.getInputStream(), enc == null ? UTF_8.name() : enc.name()));
      } else {
        source = new StAXSource(inputFactory.createXMLStreamReader(inputs.getReader()));
      }

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      return convert(source, outputFactory);
    } catch (Exception ex) {
      throw new TransformerException(this, ex);
    } finally {
      IOUtils.closeQuietly(inputs.getInputStream());
      IOUtils.closeQuietly(inputs.getReader());
    }
  }
}
