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
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.XMLEntityCodec;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Decodes a String or byte[] containing XML entities
 */
public class XmlEntityDecoder extends AbstractTransformer {

  public XmlEntityDecoder() {
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    setReturnDataType(DataType.STRING);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    try {
      String data;

      if (src instanceof byte[]) {
        data = new String((byte[]) src, encoding);
      } else if (src instanceof InputStream) {
        data = IOUtils.toString((InputStream) src, encoding);
      } else {
        data = (String) src;
      }

      return XMLEntityCodec.decodeString(data);
    } catch (Exception ex) {
      throw new TransformerException(CoreMessages.transformFailed(src.getClass().getName(), "XML"), this, ex);
    }
  }

}
