/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.message.OutputHandler;
import org.mule.runtime.core.privileged.transformer.simple.SerializableToByteArray;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <code>ObjectToByteArray</code> converts serilaizable object to a byte array but treats <code>java.lang.String</code>
 * differently by converting to bytes using the <code>String.getBytes()</code> method.
 */
public class ObjectToByteArray extends SerializableToByteArray {

  public ObjectToByteArray() {
    this.registerSourceType(DataType.INPUT_STREAM);
    this.registerSourceType(DataType.STRING);
    this.registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    this.registerSourceType(fromType(OutputHandler.class));
    this.registerSourceType(fromType(Byte.class));
    this.registerSourceType(fromType(byte.class));
    setReturnDataType(DataType.BYTE_ARRAY);
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    try {
      if (src instanceof String) {
        return src.toString().getBytes(outputEncoding);
      } else if (src instanceof CursorStreamProvider) {
        return transformStream(((CursorStreamProvider) src).openCursor());
      } else if (src instanceof InputStream) {
        return transformStream((InputStream) src);
      } else if (src instanceof OutputHandler) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
          ((OutputHandler) src).write(getCurrentEvent(), bytes);

          return bytes.toByteArray();
        } catch (IOException e) {
          throw new TransformerException(this, e);
        }
      } else if (src instanceof Byte) {
        return new byte[] {(byte) src};
      }
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }

    return super.doTransform(src, outputEncoding);
  }

  private Object transformStream(InputStream is) throws IOException {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    try {
      IOUtils.copyLarge(is, byteOut);
    } finally {
      is.close();
    }
    return byteOut.toByteArray();
  }
}
