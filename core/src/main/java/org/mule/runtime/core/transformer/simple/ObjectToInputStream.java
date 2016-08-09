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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * <code>ObjectToInputStream</code> converts Serializable objects to an InputStream but treats <code>java.lang.String</code>,
 * <code>byte[]</code> and <code>org.mule.runtime.core.message.OutputHandler</code> differently by using their byte[] content
 * rather thqn Serializing them.
 */
public class ObjectToInputStream extends SerializableToByteArray {

  public ObjectToInputStream() {
    this.registerSourceType(DataType.STRING);
    this.registerSourceType(DataType.fromType(OutputHandler.class));
    setReturnDataType(DataType.INPUT_STREAM);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    try {
      if (src instanceof String) {
        return new ByteArrayInputStream(((String) src).getBytes(encoding));
      } else if (src instanceof byte[]) {
        return new ByteArrayInputStream((byte[]) src);
      } else if (src instanceof OutputHandler) {
        OutputHandler oh = (OutputHandler) src;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        oh.write(RequestContext.getEvent(), out);

        return new ByteArrayInputStream(out.toByteArray());
      } else {
        return new ByteArrayInputStream((byte[]) super.doTransform(src, encoding));
      }
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }

}
