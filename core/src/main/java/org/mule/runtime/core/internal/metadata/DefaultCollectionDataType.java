/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

import java.util.Objects;

/**
 * A data type that represents a generified collection.
 * <p>
 * When checked for compatibility both the collection type and the generic item type will be compared.
 *
 * @since 3.0
 */
public class DefaultCollectionDataType extends SimpleDataType implements CollectionDataType {

  private static final long serialVersionUID = 3600944898597616006L;

  private final DataType itemsType;

  DefaultCollectionDataType(Class collectionType, DataType type, MediaType mimeType, boolean streamType) {
    super(collectionType, mimeType, streamType);
    this.itemsType = type;
  }

  @Override
  public DataType getItemDataType() {
    return itemsType;
  }

  @Override
  public boolean isCompatibleWith(DataType dataType) {
    if (!(dataType instanceof DefaultCollectionDataType)) {
      return false;
    }

    if (!super.isCompatibleWith(dataType)) {
      return false;
    }
    DefaultCollectionDataType that = (DefaultCollectionDataType) dataType;

    return getItemDataType().isCompatibleWith(that.getItemDataType());

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultCollectionDataType that = (DefaultCollectionDataType) o;

    return Objects.equals(this.getItemDataType(), that.getItemDataType()) &&
        Objects.equals(this.getType(), that.getType()) &&
        Objects.equals(this.getMediaType(), that.getMediaType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getItemDataType(), getMediaType());
  }

  @Override
  public String toString() {
    return "CollectionDataType{" + "type=" + getType().getName() + ", itemType=" + getItemDataType().toString() + ", mimeType='"
        + getMediaType() + '\'' + '}';
  }

}
