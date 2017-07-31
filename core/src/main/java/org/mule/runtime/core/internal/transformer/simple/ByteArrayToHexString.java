/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.util.StringUtils;

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
    registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
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
      if (src instanceof CursorStreamProvider) {
        bytes = handleStream(((CursorStreamProvider) src).openCursor());
      } else if (src instanceof InputStream) {
        bytes = handleStream((InputStream) src);
      } else {
        bytes = (byte[]) src;
      }

      return StringUtils.toHexString(bytes, upperCase);
    } catch (Exception ex) {
      throw new TransformerException(this, ex);
    }
  }

  private byte[] handleStream(InputStream input) {
    try {
      return toByteArray(input);
    } finally {
      closeQuietly(input);
    }
  }
}
