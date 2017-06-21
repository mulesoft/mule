/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.codec;

import static org.apache.commons.io.IOUtils.toByteArray;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.api.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * <code>Base64Encoder</code> transforms strings or byte arrays into Base64 encoded string.
 */
public class Base64Encoder extends AbstractTransformer {

  public Base64Encoder() {
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    setReturnDataType(DataType.STRING);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    try {
      byte[] buf;

      if (src instanceof String) {
        buf = ((String) src).getBytes(encoding);
      } else if (src instanceof CursorStreamProvider) {
        buf = handleStream(((CursorStreamProvider) src).openCursor(), encoding);
      } else if (src instanceof InputStream) {
        buf = handleStream((InputStream) src, encoding);
      } else {
        buf = (byte[]) src;
      }

      String result = Base64.encodeBytes(buf, Base64.DONT_BREAK_LINES);

      if (byte[].class.isAssignableFrom(getReturnDataType().getType())) {
        return result.getBytes(encoding);
      } else {
        return result;
      }
    } catch (Exception ex) {
      throw new TransformerException(CoreMessages.transformFailed(src.getClass().getName(), "base64"), this, ex);
    }
  }

  private byte[] handleStream(InputStream src, Charset encoding) throws IOException {
    InputStreamReader input = new InputStreamReader(src);
    try {
      return toByteArray(input, encoding);
    } finally {
      input.close();
    }
  }

}
