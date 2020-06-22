/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.compression;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.serialization.SerializationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.util.compression.AbstractCompressionTransformer;
import org.mule.runtime.core.api.util.compression.GZipCompression;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


/**
 * <code>GZipCompressTransformer</code> will uncompress a byte[] or InputStream
 */
public class GZipUncompressTransformer extends AbstractCompressionTransformer {

  public GZipUncompressTransformer() {
    super();
    this.setStrategy(new GZipCompression());
    this.registerSourceType(DataType.BYTE_ARRAY);
    this.registerSourceType(DataType.INPUT_STREAM);
    this.registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    // No type checking for the return type by default. It could either be a byte array, an input stream or an object.
    this.setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    try {
      if (src instanceof CursorStreamProvider) {
        return getStrategy().uncompressInputStream(((CursorStreamProvider) src).openCursor());
      }
      if (src instanceof InputStream) {
        return getStrategy().uncompressInputStream((InputStream) src);
      } else {
        byte[] buffer = getStrategy().uncompressByteArray((byte[]) src);
        DataType returnDataType = getReturnDataType();

        // If a return type has been specified, then deserialize the uncompressed byte array.
        if (DataType.STRING.isCompatibleWith(returnDataType)) {
          return new String(buffer, outputEncoding);
        } else if (!DataType.OBJECT.isCompatibleWith(returnDataType) && !DataType.BYTE_ARRAY.isCompatibleWith(returnDataType)) {
          try {
            return muleContext.getObjectSerializer().getExternalProtocol().deserialize(buffer);
          } catch (SerializationException e) {
            throw new TransformerException(this, e);
          }
        } else {
          // First try to deserialize the byte array. If it can be deserialized, then it was originally serialized.
          try {
            return muleContext.getObjectSerializer().getExternalProtocol().deserialize(buffer);
          } catch (SerializationException e) {
            // If it fails, ignore it. We assume it was not serialized in the first place and return the buffer as it is.
            return buffer;
          }
        }
      }
    } catch (IOException e) {
      throw new TransformerException(I18nMessageFactory.createStaticMessage("Failed to uncompress message."), this, e);
    }
  }
}
