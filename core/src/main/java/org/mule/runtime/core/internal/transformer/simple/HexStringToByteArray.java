/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.internal.util.ArrayUtils;
import org.mule.runtime.core.api.util.StringUtils;

import java.nio.charset.Charset;

/**
 * Converts a Hex String to a Byte array
 */
public class HexStringToByteArray extends AbstractTransformer {

  public HexStringToByteArray() {
    registerSourceType(DataType.STRING);
    setReturnDataType(DataType.BYTE_ARRAY);
  }

  @Override
  protected Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    if (src == null) {
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }

    try {
      return StringUtils.hexStringToByteArray((String) src);
    } catch (Exception ex) {
      throw new TransformerException(this, ex);
    }
  }

}
