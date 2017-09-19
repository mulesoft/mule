/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.privileged.util.NumberUtils;

import java.nio.charset.Charset;
import java.text.NumberFormat;

/**
 * <code>StringToNumber</code> converts a String to a Number. A NumberFormat is used if one is provided.
 */
public class StringToNumber extends AbstractTransformer implements DiscoverableTransformer {

  private NumberFormat numberFormat;

  /**
   * Give core transformers a slightly higher priority
   */
  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

  public StringToNumber() {
    registerSourceType(DataType.STRING);
    setReturnDataType(DataType.NUMBER);
  }

  public StringToNumber(NumberFormat numberFormat) {
    this();
    this.numberFormat = numberFormat;
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    if (src == null) {
      return null;
    } else {
      if (numberFormat != null) {
        try {
          return NumberUtils.convertNumberToTargetClass(numberFormat.parse((String) src), getType());
        } catch (Exception e) {
          throw new TransformerException(this, e);
        }
      } else {
        return NumberUtils.parseNumber((String) src, getType());
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  private Class<? extends Number> getType() {
    return (Class<Number>) super.getReturnDataType().getType();
  }

  @Override
  public void setReturnDataType(DataType type) {
    if (!Number.class.isAssignableFrom(type.getType())) {
      throw new IllegalArgumentException("This transformer only supports Number return types.");
    } else {
      super.setReturnDataType(type);
    }
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
