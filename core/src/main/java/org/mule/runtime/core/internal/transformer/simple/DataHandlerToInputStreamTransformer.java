/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
