/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

import java.util.Objects;

/**
 * A data type that simply wraps a Java type.
 * <p>
 * This type also allows a mime type and encoding to be associated with the Java type.
 *
 * @since 1.0
 */
public class SimpleDataType implements DataType {

  private static final long serialVersionUID = -4590745924720880358L;

  protected final Class<?> type;
  protected final MediaType mimeType;
  protected final boolean streamType;

  SimpleDataType(Class<?> type, MediaType mimeType, boolean streamType) {
    this.type = type;
    this.mimeType = mimeType;
    this.streamType = streamType;
  }

  @Override
  public Class<?> getType() {
    return type;
  }

  @Override
  public MediaType getMediaType() {
    return mimeType;
  }

  private Class<?> fromPrimitive(Class<?> type) {
    Class<?> primitiveWrapper = getPrimitiveWrapper(type);
    if (primitiveWrapper != null) {
      return primitiveWrapper;
    } else {
      return type;
    }
  }

  private Class<?> getPrimitiveWrapper(Class<?> primitiveType) {
    if (boolean.class.equals(primitiveType)) {
      return Boolean.class;
    } else if (float.class.equals(primitiveType)) {
      return Float.class;
    } else if (long.class.equals(primitiveType)) {
      return Long.class;
    } else if (int.class.equals(primitiveType)) {
      return Integer.class;
    } else if (short.class.equals(primitiveType)) {
      return Short.class;
    } else if (byte.class.equals(primitiveType)) {
      return Byte.class;
    } else if (double.class.equals(primitiveType)) {
      return Double.class;
    } else if (char.class.equals(primitiveType)) {
      return Character.class;
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SimpleDataType that = (SimpleDataType) o;

    return Objects.equals(this.getType(), that.getType()) &&
        Objects.equals(this.getMediaType(), that.getMediaType());
  }

  @Override
  public boolean isCompatibleWith(DataType dataType) {
    if (this == dataType) {
      return true;
    }
    if (dataType == null) {
      return false;
    }

    SimpleDataType that = (SimpleDataType) dataType;

    if (!fromPrimitive(this.getType()).isAssignableFrom(fromPrimitive(that.getType()))) {
      return false;
    }

    return MediaType.ANY.matches(getMediaType()) || mediaTypesMatch(dataType);
  }

  private boolean mediaTypesMatch(DataType other) {

    if (this.getMediaType() == null && other.getMediaType() != null) {
      return false;
    }

    if (this.getMediaType() != null && other.getMediaType() == null) {
      return false;
    }

    if (!this.getMediaType().getCharset().isPresent()) {
      return this.getMediaType().matches(other.getMediaType());
    } else {
      return this.getMediaType().equals(other.getMediaType());
    }

  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getMediaType());
  }


  @Override
  public String toString() {
    return "SimpleDataType{" + "type=" + (type == null ? null : type.getName()) + ", mimeType='" + mimeType + '\'' + '}';
  }

  @Override
  public boolean isStreamType() {
    return streamType;
  }
}
