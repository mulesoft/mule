/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.codec;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.internal.util.XMLEntityCodec;

import java.io.InputStream;
import java.nio.charset.Charset;


/**
 * Encodes a string with XML entities
 */
public class XmlEntityEncoder extends AbstractTransformer {

  public XmlEntityEncoder() {
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    setReturnDataType(DataType.STRING);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    try {
      String data;

      if (src instanceof byte[]) {
        data = new String((byte[]) src, encoding);
      } else if (src instanceof CursorStreamProvider) {
        try (InputStream in = ((CursorStreamProvider) src).openCursor()) {
          data = org.apache.commons.io.IOUtils.toString(in, encoding);
        }
      } else if (src instanceof InputStream) {
        data = org.apache.commons.io.IOUtils.toString((InputStream) src, encoding);
      } else {
        data = (String) src;
      }

      return XMLEntityCodec.encodeString(data);
    } catch (Exception ex) {
      throw new TransformerException(CoreMessages.transformFailed(src.getClass().getName(), "XML"), this, ex);
    }
  }

}
