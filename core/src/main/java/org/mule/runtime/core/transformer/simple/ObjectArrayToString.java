/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.apache.commons.lang.StringUtils.join;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.nio.charset.Charset;

/**
 * <code>ObjectArrayToString</code> transformer is the opposite of StringToObjectArray - it simply converts Object[] to a String
 * in which each element is separated by a configurable delimiter (default is a space).
 */

public class ObjectArrayToString extends AbstractTransformer implements DiscoverableTransformer {

  /** Give core transformers a slighty higher priority */
  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

  private static final String DEFAULT_DELIMITER = " ";

  private String delimiter = null;

  public ObjectArrayToString() {
    registerSourceType(DataType.fromType(Object[].class));
    setReturnDataType(DataType.STRING);
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    if (src == null) {
      return src;
    }

    Object[] in = (Object[]) src;
    String out = join(in, getDelimiter());

    /*
     * for (int i = 0; i < in.length; i++) { if (in[i] != null) { if (i > 0) out += getDelimiter(); out += in[i].toString(); } }
     */

    return out;
  }

  public String getDelimiter() {
    if (delimiter == null) {
      return DEFAULT_DELIMITER;
    } else {
      return delimiter;
    }
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int priorityWeighting) {
    this.priorityWeighting = priorityWeighting;
  }
}
