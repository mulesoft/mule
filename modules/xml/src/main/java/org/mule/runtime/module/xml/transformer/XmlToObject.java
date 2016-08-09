/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * <code>XmlToObject</code> converts xml created by the ObjectToXml transformer in to a java object graph. This transformer uses
 * XStream. Xstream uses some clever tricks so objects that get marshalled to XML do not need to implement any interfaces
 * including Serializable and you don't even need to specify a default constructor.
 *
 * @see ObjectToXml
 */

public class XmlToObject extends AbstractXStreamTransformer {

  private final DomDocumentToXml domTransformer = new DomDocumentToXml();

  public XmlToObject() {
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.fromType(org.w3c.dom.Document.class));
    registerSourceType(DataType.fromType(org.dom4j.Document.class));
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    Object src = event.getMessage().getPayload();
    Object result;
    if (src instanceof byte[]) {
      Reader xml = new InputStreamReader(new ByteArrayInputStream((byte[]) src), outputEncoding);
      result = getXStream().fromXML(xml);
    } else if (src instanceof InputStream) {
      InputStream input = (InputStream) src;
      try {
        Reader xml = new InputStreamReader(input, outputEncoding);
        result = getXStream().fromXML(xml);
      } catch (Exception e) {
        throw new TransformerException(this, e);
      } finally {
        try {
          input.close();
        } catch (IOException e) {
          logger.warn("Exception closing stream: ", e);
        }
      }
    } else if (src instanceof String) {
      result = getXStream().fromXML(src.toString());
    } else {
      result = getXStream().fromXML((String) domTransformer.transform(src));
    }

    try {
      postDeserialisationInit(result);
      return result;
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }

  protected void postDeserialisationInit(final Object object) throws Exception {
    if (object instanceof DeserializationPostInitialisable) {
      DeserializationPostInitialisable.Implementation.init(object, muleContext);
    }
  }

}
