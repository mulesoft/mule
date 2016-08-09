/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.metadata;

import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

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

  DefaultCollectionDataType(Class<? extends Iterable> collectionType, DataType type, MediaType mimeType) {
    super(collectionType, mimeType);
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

    if (!getItemDataType().equals(that.getItemDataType())) {
      return false;
    }

    // TODO MULE-9987 Fix this
    if ((mimeType != null ? !mimeType.matches(that.mimeType) : that.mimeType != null) && !MediaType.ANY.matches(that.mimeType)
        && !MediaType.ANY.matches(this.mimeType)) {
      return false;
    }

    return getType().equals(that.getType());

  }

  @Override
  public int hashCode() {
    int result = getType().hashCode();
    result = 31 * result + getItemDataType().hashCode();
    result = 31 * result + (getMediaType() != null ? getMediaType().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "CollectionDataType{" + "type=" + getType().getName() + ", itemType=" + getItemDataType().toString() + ", mimeType='"
        + getMediaType() + '\'' + '}';
  }
}
