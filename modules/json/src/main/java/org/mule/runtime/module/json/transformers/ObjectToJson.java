/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.transformers;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.module.json.filters.IsJsonFilter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a java object to a JSON encoded object that can be consumed by other languages such as Javascript or Ruby.
 * <p/>
 * The returnClass for this transformer is always java.lang.String, there is no need to set this.
 */
public class ObjectToJson extends AbstractJsonTransformer {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(ObjectToJson.class);

  private Map<Class<?>, Class<?>> serializationMixins = new HashMap<>();

  protected Class<?> sourceClass;

  private boolean handleException = false;

  private IsJsonFilter isJsonFilter = new IsJsonFilter();

  public ObjectToJson() {
    this.setReturnDataType(DataType.JSON_STRING);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();

    // restrict the handled types
    if (getSourceClass() != null) {
      sourceTypes.clear();
      registerSourceType(DataType.fromType(getSourceClass()));
    }

    // Add shared mixins first
    for (Map.Entry<Class<?>, Class<?>> entry : getMixins().entrySet()) {
      getMapper().getSerializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<Class<?>, Class<?>> entry : serializationMixins.entrySet()) {
      getMapper().getSerializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    Object src = event.getMessage().getPayload();
    if (src instanceof String && isJsonFilter.accept(src)) {
      // Nothing to transform
      return src;
    }

    // Checks if there's an exception
    if (event.getError() != null && this.isHandleException()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found exception with null payload");
      }
      src = this.getException(event.getError().getException());
    }

    StringWriter writer = new StringWriter();
    try {
      getMapper().writeValue(writer, src);
    } catch (IOException e) {
      throw new TransformerException(this, e);
    }

    if (byte[].class.equals(getReturnDataType().getType())) {
      return writer.toString().getBytes(outputEncoding);
    } else {
      return writer.toString();
    }
  }

  /**
   * The reason of having this is because the original exception object is way too complex and it breaks JSON-lib.
   */
  private Exception getException(Throwable t) {
    Exception returnValue = null;
    List<Throwable> causeStack = new ArrayList<>();

    for (Throwable tempCause = t; tempCause != null; tempCause = tempCause.getCause()) {
      causeStack.add(tempCause);
    }

    for (int i = causeStack.size() - 1; i >= 0; i--) {
      Throwable tempCause = causeStack.get(i);

      // There is no cause at the very root
      if (i == causeStack.size()) {
        returnValue = new Exception(tempCause.getMessage());
        returnValue.setStackTrace(tempCause.getStackTrace());
      } else {
        returnValue = new Exception(tempCause.getMessage(), returnValue);
        returnValue.setStackTrace(tempCause.getStackTrace());
      }
    }

    return returnValue;
  }

  public boolean isHandleException() {
    return this.handleException;
  }

  public void setHandleException(boolean handleException) {
    this.handleException = handleException;
  }

  public Class<?> getSourceClass() {
    return sourceClass;
  }

  public void setSourceClass(Class<?> sourceClass) {
    this.sourceClass = sourceClass;
  }

  public Map<Class<?>, Class<?>> getSerializationMixins() {
    return serializationMixins;
  }

  public void setSerializationMixins(Map<Class<?>, Class<?>> serializationMixins) {
    this.serializationMixins = serializationMixins;
  }
}

