/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.codec;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.Base64;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <code>Base64Encoder</code> transforms Base64 encoded data into strings or byte arrays.
 */
public class Base64Decoder extends AbstractTransformer {

  public Base64Decoder() {
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    setReturnDataType(DataType.BYTE_ARRAY);
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    try {
      String data;

      if (src instanceof byte[]) {
        data = new String((byte[]) src, outputEncoding);
      } else if (src instanceof InputStream) {
        InputStream input = (InputStream) src;
        try {
          data = IOUtils.toString(input, outputEncoding);
        } finally {
          input.close();
        }
      } else {
        data = (String) src;
      }

      byte[] result = Base64.decode(data);

      if (DataType.STRING.isCompatibleWith(getReturnDataType())) {
        return new String(result, outputEncoding);
      } else {
        return result;
      }
    } catch (Exception ex) {
      throw new TransformerException(CoreMessages.transformFailed("base64", getReturnDataType()), this, ex);
    }
  }

}
