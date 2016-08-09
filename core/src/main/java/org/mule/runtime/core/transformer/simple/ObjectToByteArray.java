/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.IOUtils;

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
    this.registerSourceType(DataType.fromType(OutputHandler.class));
    setReturnDataType(DataType.BYTE_ARRAY);
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    try {
      if (src instanceof String) {
        return src.toString().getBytes(outputEncoding);
      } else if (src instanceof InputStream) {
        InputStream is = (InputStream) src;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
          IOUtils.copyLarge(is, byteOut);
        } finally {
          is.close();
        }
        return byteOut.toByteArray();
      } else if (src instanceof OutputHandler) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
          ((OutputHandler) src).write(RequestContext.getEvent(), bytes);

          return bytes.toByteArray();
        } catch (IOException e) {
          throw new TransformerException(this, e);
        }
      }
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }

    return super.doTransform(src, outputEncoding);
  }
}
