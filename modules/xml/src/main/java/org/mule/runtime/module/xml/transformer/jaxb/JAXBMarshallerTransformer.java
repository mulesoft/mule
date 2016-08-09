/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer.jaxb;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * Allows marshaling of Java objects to XML using JAXB 2. A specific sourceClass can be set on this transformer, this is the
 * expected source object type. If no external {@link javax.xml.bind.JAXBContext} is set on the transformer, but the 'sourceClass'
 * is set, a {@link javax.xml.bind.JAXBContext} will be created using the sourceClass.
 *
 * @since 3.0
 */
public class JAXBMarshallerTransformer extends AbstractTransformer {

  protected JAXBContext jaxbContext;

  protected Class<?> sourceClass;

  public JAXBMarshallerTransformer() {
    setReturnDataType(DataType.fromType(OutputStream.class));
    registerSourceType(DataType.OBJECT);
  }

  public JAXBMarshallerTransformer(JAXBContext jaxbContext, DataType returnType) {
    this();
    this.jaxbContext = jaxbContext;
    setReturnDataType(returnType);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    if (jaxbContext == null) {
      throw new InitialisationException(CoreMessages.objectIsNull("jaxbContext"), this);
    }
  }

  @Override
  protected Object doTransform(final Object src, Charset encoding) throws TransformerException {
    try {
      final Marshaller m = jaxbContext.createMarshaller();
      if (String.class.isAssignableFrom(getReturnDataType().getType())) {
        Writer w = new StringWriter();
        m.marshal(src, w);
        return w.toString();
      } else if (getReturnDataType().getType().isAssignableFrom(Writer.class)) {
        Writer w = new StringWriter();
        m.marshal(src, w);
        return w;
      } else if (Document.class.isAssignableFrom(getReturnDataType().getType())) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().newDocument();
        m.marshal(src, doc);
        return doc;
      } else if (OutputStream.class.isAssignableFrom(getReturnDataType().getType())) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.marshal(src, out);
        return out;
      } else if (OutputHandler.class.isAssignableFrom(getReturnDataType().getType())) {
        return (OutputHandler) (event, out) -> {
          try {
            m.marshal(src, out);
          } catch (JAXBException e) {
            IOException iox = new IOException("failed to mashal objec tto XML");
            iox.initCause(e);
            throw iox;
          }
        };
      } else {
        throw new TransformerException(CoreMessages.transformerInvalidReturnType(getReturnDataType().getType(), getName()));
      }

    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }

  public JAXBContext getJaxbContext() {
    return jaxbContext;
  }

  public void setJaxbContext(JAXBContext jaxbContext) {
    this.jaxbContext = jaxbContext;
  }

  public Class<?> getSourceClass() {
    return sourceClass;
  }

  public void setSourceClass(Class<?> sourceClass) {
    this.sourceClass = sourceClass;
  }
}
