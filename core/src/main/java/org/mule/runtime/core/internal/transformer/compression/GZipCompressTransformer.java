/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.compression;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.compression.AbstractCompressionTransformer;
import org.mule.runtime.core.api.util.compression.GZipCompression;

import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * <code>GZipCompressTransformer</code> is a transformer compressing objects into byte arrays.
 */
public class GZipCompressTransformer extends AbstractCompressionTransformer {

  public GZipCompressTransformer() {
    super();
    this.setStrategy(new GZipCompression());
    this.registerSourceType(DataType.fromType(Serializable.class));
    this.registerSourceType(DataType.BYTE_ARRAY);
    this.registerSourceType(DataType.INPUT_STREAM);
    this.registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    // No type checking for the return type by default. It could either be a byte array or an input stream.
    this.setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    try {
      if (src instanceof CursorStreamProvider) {
        return getStrategy().compressInputStream(((CursorStreamProvider) src).openCursor());
      }
      if (src instanceof InputStream) {
        return getStrategy().compressInputStream((InputStream) src);
      } else {
        byte[] data;
        if (src instanceof byte[]) {
          data = (byte[]) src;
        } else if (src instanceof String) {
          data = ((String) src).getBytes(outputEncoding);
        } else {
          data = muleContext.getObjectSerializer().getExternalProtocol().serialize(src);
        }
        return getStrategy().compressByteArray(data);
      }
    } catch (Exception ioex) {
      throw new TransformerException(this, ioex);
    }
  }
}
