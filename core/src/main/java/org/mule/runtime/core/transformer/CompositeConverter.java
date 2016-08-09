/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * Composes many converters to behave as a single one.
 * <p/>
 * When {@link #transform(Object)} is called each converter in the same order they are included in the composition. The output of
 * a given converter is the input of the next composed converter.
 */
public class CompositeConverter implements Converter {

  private String name;

  private LinkedList<Converter> chain;
  private TransformationService transformationService;

  /**
   * Create a new conversion chain using the specified converters
   *
   * @param converters List of converters using to build the chain
   */
  public CompositeConverter(Converter... converters) {
    if (converters.length == 0) {
      throw new IllegalArgumentException("There must be at least one converter");
    }

    chain = new LinkedList<>();

    name = compositeConverterName(converters);
    this.transformationService = transformationService;
  }

  private String compositeConverterName(Converter[] converters) {
    StringBuilder builder = new StringBuilder();
    for (Converter converter : converters) {
      chain.addLast(converter);
      builder.append(converter.getName());
    }

    return builder.toString();
  }

  @Override
  public boolean isSourceDataTypeSupported(DataType dataType) {
    return chain.size() > 0 && chain.peekFirst().isSourceDataTypeSupported(dataType);
  }

  @Override
  public List<DataType> getSourceDataTypes() {
    return chain.peekFirst().getSourceDataTypes();
  }

  @Override
  public boolean isAcceptNull() {
    return chain.size() > 0 && chain.peekFirst().isAcceptNull();
  }

  @Override
  public boolean isIgnoreBadInput() {
    return chain.size() > 0 && chain.peekFirst().isIgnoreBadInput();
  }

  @Override
  public Object transform(Object src) throws TransformerException {
    return transform(src, null);
  }

  @Override
  public Object transform(Object src, Charset encoding) throws TransformerException {
    Object current = src;
    Charset currentEncoding = encoding;
    for (Converter converter : chain) {
      if (currentEncoding != null) {
        current = converter.transform(current, currentEncoding);
      } else {
        current = converter.transform(current);
      }
      currentEncoding = converter.getReturnDataType().getMediaType().getCharset().orElse(encoding);
    }

    return current;
  }

  @Override
  public void setReturnDataType(DataType type) {
    chain.peekLast().setReturnDataType(type);
  }

  @Override
  public DataType getReturnDataType() {
    return chain.peekLast().getReturnDataType();
  }

  @Override
  public void dispose() {
    for (Converter converter : chain) {
      converter.dispose();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    for (Converter converter : chain) {
      converter.initialise();
    }
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (event != null && event.getMessage() != null) {
      try {
        event.setMessage(event.getMuleContext().getTransformationService().applyTransformers(event.getMessage(), event, this));
      } catch (Exception e) {
        throw new TransformerMessagingException(event, this, e);
      }
    }

    return event;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    for (Converter converter : chain) {
      converter.setMuleContext(context);
    }
  }

  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException("Cannot change composite converter name");
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getPriorityWeighting() {
    int priorityWeighting = 0;
    for (Converter converter : chain) {
      priorityWeighting += converter.getPriorityWeighting();
    }

    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int weighting) {}

  public LinkedList<Converter> getConverters() {
    return new LinkedList<>(chain);
  }
}
