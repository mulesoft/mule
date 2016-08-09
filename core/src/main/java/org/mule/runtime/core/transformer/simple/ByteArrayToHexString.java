/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.Charset;


/**
 * Converts a Byte array to a Hex String.
 */
public class ByteArrayToHexString extends AbstractTransformer {

  private volatile boolean upperCase = false;

  public ByteArrayToHexString() {
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    setReturnDataType(DataType.STRING);
  }

  public boolean getUpperCase() {
    return upperCase;
  }

  public void setUpperCase(boolean value) {
    upperCase = value;
  }

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    if (src == null) {
      return StringUtils.EMPTY;
    }

    try {
      byte[] bytes = null;
      if (src instanceof InputStream) {
        InputStream input = (InputStream) src;
        try {
          bytes = IOUtils.toByteArray(input);
        } finally {
          IOUtils.closeQuietly(input);
        }
      } else {
        bytes = (byte[]) src;
      }

      return StringUtils.toHexString(bytes, upperCase);
    } catch (Exception ex) {
      throw new TransformerException(this, ex);
    }
  }

}
