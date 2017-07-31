/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

public class StringAppendTransformer extends AbstractTransformer {

  private String message = StringUtils.EMPTY;

  public StringAppendTransformer() {
    this(StringUtils.EMPTY);
  }

  public StringAppendTransformer(String message) {
    this.message = message;
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    setReturnDataType(DataType.STRING);
  }

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    String string;
    if (src instanceof byte[]) {
      string = new String((byte[]) src);
    } else if (src instanceof CursorStreamProvider) {
      string = handleStream(((CursorStreamProvider) src).openCursor());
    } else if (src instanceof InputStream) {
      string = handleStream((InputStream) src);
    } else {
      string = (String) src;
    }

    return append(message, string);
  }

  private String handleStream(InputStream input) {
    String string;
    try {
      string = IOUtils.toString(input);
    } finally {
      closeQuietly(input);
    }
    return string;
  }

  public static String append(String append, String msg) {
    return msg + append;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
