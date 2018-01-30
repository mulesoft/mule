/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer.simple;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint;
import static org.mule.runtime.core.api.util.IOUtils.ifInputStream;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.NotAnInputStreamException;

import java.io.IOException;
import java.io.ObjectStreamConstants;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;

/**
 * <code>ByteArrayToObject</code> works in the same way as <code>ByteArrayToSerializable</code> but checks if the byte array is a
 * serialised object and if not will return a String created from the bytes as the returnType on the transformer.
 */
public final class ByteArrayToObject extends ByteArrayToSerializable {

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    if (src instanceof byte[]) {
      byte[] bytes = (byte[]) src;

      if (this.checkStreamHeader(bytes[0])) {
        return super.doTransform(src, encoding);
      } else {
        return new String(bytes, encoding);
      }
    } else {
      try {
        return ifInputStream(src, stream -> {
          try {
            PushbackInputStream pushbackStream = new PushbackInputStream(stream);
            int firstByte = pushbackStream.read();
            pushbackStream.unread((byte) firstByte);

            if (this.checkStreamHeader((byte) firstByte)) {
              return super.doTransform(pushbackStream, encoding);
            } else {
              try {
                return org.apache.commons.io.IOUtils.toString(pushbackStream, encoding);
              } finally {
                // this also closes the underlying stream that's stored in src
                pushbackStream.close();
              }
            }
          } catch (IOException iox) {
            throw new TransformerException(this, iox);
          }
        });
      } catch (NotAnInputStreamException e) {
        throw new TransformerException(transformOnObjectUnsupportedTypeOfEndpoint(this.getName(), src.getClass()));
      }
    }
  }

  private boolean checkStreamHeader(byte firstByte) {
    return (firstByte == (byte) ((ObjectStreamConstants.STREAM_MAGIC >>> 8) & 0xFF));
  }
}
