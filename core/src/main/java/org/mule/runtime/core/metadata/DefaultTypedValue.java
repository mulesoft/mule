/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.metadata;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.Serializable;

/**
 * Maintains a content that has an associated {@link DataType}
 */
public class DefaultTypedValue<T> implements TypedValue<T>, Serializable {

  private static final long serialVersionUID = -2533879516750283994L;

  private final T value;
  private final DataType dataType;

  public DefaultTypedValue(T value, DataType dataType) {
    this.value = value;
    if (dataType == null) {
      this.dataType = DataType.fromObject(value);
    } else {
      this.dataType = dataType;
    }
  }

  @Override
  public DataType getDataType() {
    return dataType;
  }

  @Override
  public T getValue() {
    return value;
  }

}
