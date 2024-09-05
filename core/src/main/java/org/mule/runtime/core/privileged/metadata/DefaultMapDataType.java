/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.metadata;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.runtime.api.metadata.MediaType;

import java.util.Objects;

/**
 * Implementation of {@link MapDataType}.
 *
 * @since 4.6, moved from {@link org.mule.runtime.core.internal.metadata.DefaultMapDataType}.
 */
@NoExtend
public class DefaultMapDataType extends SimpleDataType implements MapDataType {

  private static final long serialVersionUID = 1052687171949146300L;

  private final DataType keyType;
  private final DataType valueType;

  protected DefaultMapDataType(Class<?> type, DataType keyType, DataType valueType, MediaType mimeType, boolean streamType) {
    super(type, mimeType, streamType);
    this.keyType = keyType;
    this.valueType = valueType;
  }

  @Override
  public boolean isCompatibleWith(DataType dataType) {
    if (dataType instanceof DynamicDelegateDataType) {
      dataType = ((DynamicDelegateDataType) dataType).getDelegate();
    }
    if (!(dataType instanceof DefaultMapDataType)) {
      return false;
    }

    if (!super.isCompatibleWith(dataType)) {
      return false;
    }
    DefaultMapDataType that = (DefaultMapDataType) dataType;

    return getKeyDataType().isCompatibleWith(that.getKeyDataType())
        && getValueDataType().isCompatibleWith(that.getValueDataType());
  }

  @Override
  public DataType getKeyDataType() {
    return keyType;
  }

  @Override
  public DataType getValueDataType() {
    return valueType;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DynamicDelegateDataType) {
      o = ((DynamicDelegateDataType) o).getDelegate();
    }
    if (this == o) {
      return true;
    }
    if (o == null || !equalsCheckClass(o)) {
      return false;
    }

    DefaultMapDataType that = (DefaultMapDataType) o;

    return Objects.equals(this.getKeyDataType(), that.getKeyDataType()) &&
        Objects.equals(this.getValueDataType(), that.getValueDataType()) &&
        Objects.equals(this.getType(), that.getType()) &&
        Objects.equals(this.getMediaType(), that.getMediaType());
  }

  @Override
  protected boolean equalsCheckClass(Object o) {
    return getClass() == o.getClass()
        || (DefaultMapDataType.class.isAssignableFrom(o.getClass()));
  }

  @Override
  public int hashCode() {
    // No need to consider `DynamicDelegateDataType` for hashcode calculation as we're only interested in its delegate
    return Objects.hash(getType(), getKeyDataType(), getValueDataType(), getMediaType());
  }

  @Override
  public String toString() {
    return "MapDataType{" + "type=" + getType().getName() + ", keyType=" + getKeyDataType().toString() + ", valueType="
        + getValueDataType().toString() + ", mimeType='" + getMediaType() + '\'' + '}';
  }

}
