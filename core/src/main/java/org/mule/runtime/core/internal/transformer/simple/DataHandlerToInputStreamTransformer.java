/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.transformer.AbstractDiscoverableTransformer;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.activation.DataHandler;

public class DataHandlerToInputStreamTransformer extends AbstractDiscoverableTransformer {

  public DataHandlerToInputStreamTransformer() {
    registerSourceType(DataType.fromType(DataHandler.class));
    setReturnDataType(DataType.INPUT_STREAM);
  }

  @Override
  public Object doTransform(Object src, Charset enc) throws TransformerException {
    try {
      return ((DataHandler) src).getInputStream();
    } catch (IOException e) {
      throw new TransformerException(this, e);
    }
  }
}
