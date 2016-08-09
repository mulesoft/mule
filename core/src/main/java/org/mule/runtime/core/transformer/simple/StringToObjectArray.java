/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <code>StringToObjectArray</code> converts a String into an object array. This is useful in certain situations, as when a string
 * needs to be converted into an Object[] in order to be passed to a SOAP service. The input String is parsed into the array based
 * on a configurable delimiter - default is a space.
 */
public class StringToObjectArray extends AbstractTransformer {

  private String delimiter = null;
  private static final String DEFAULT_DELIMITER = " ";

  public StringToObjectArray() {
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.INPUT_STREAM);
    setReturnDataType(DataType.fromType(Object[].class));
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    String in;

    if (src instanceof byte[]) {
      in = createStringFromByteArray((byte[]) src, outputEncoding);
    } else if (src instanceof InputStream) {
      in = createStringFromInputStream((InputStream) src);
    } else {
      in = (String) src;
    }

    String[] out = StringUtils.splitAndTrim(in, getDelimiter());
    return out;
  }

  protected String createStringFromByteArray(byte[] bytes, Charset outputEncoding) throws TransformerException {
    return new String(bytes, outputEncoding);
  }

  protected String createStringFromInputStream(InputStream input) {
    try {
      return IOUtils.toString(input);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * @return the delimiter
   */
  public String getDelimiter() {
    if (delimiter == null) {
      return DEFAULT_DELIMITER;
    } else {
      return delimiter;
    }
  }

  /**
   * @param delimiter the delimiter
   */
  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

}
