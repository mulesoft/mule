/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.transformer.AbstractDiscoverableTransformer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

import javax.activation.DataHandler;

public class DataHandlerToInputStreamTransformer extends AbstractDiscoverableTransformer {

  public DataHandlerToInputStreamTransformer() {
    this(DataHandler.class);
  }

  public DataHandlerToInputStreamTransformer(Class<?> dataHandlerType) {
    registerSourceType(DataType.fromType(dataHandlerType));
    setReturnDataType(DataType.INPUT_STREAM);
  }

  @Override
  public Object doTransform(Object src, Charset enc) throws TransformerException {
    try {
      return src.getClass().getDeclaredMethod("getInputStream").invoke(src);
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }
}
