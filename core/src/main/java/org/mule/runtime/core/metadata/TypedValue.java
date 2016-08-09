/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.metadata;

import org.mule.runtime.api.metadata.DataType;

import java.io.Serializable;

/**
 * Maintains a value that has an associated {@link DataType}
 */
public class TypedValue<T> implements Serializable {

  private static final long serialVersionUID = -2533879516750283994L;

  private final T value;
  private final DataType dataType;

  public TypedValue(T value, DataType dataType) {
    this.value = value;
    if (dataType == null) {
      this.dataType = DataType.fromObject(value);
    } else {
      this.dataType = dataType;
    }
  }

  public DataType getDataType() {
    return dataType;
  }

  public T getValue() {
    return value;
  }

}
