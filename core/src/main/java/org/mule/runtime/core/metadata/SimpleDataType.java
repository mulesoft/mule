/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.metadata;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

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

  SimpleDataType(Class<?> type, MediaType mimeType) {
    this.type = type;
    this.mimeType = mimeType;
  }

  @Override
  public Class<?> getType() {
    return type;
  }

  @Override
  public MediaType getMediaType() {
    return mimeType;
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

    // ANY_MIME_TYPE will match to a null or non-null value for MediaType
    if ((this.getMediaType() == null && that.getMediaType() != null || that.getMediaType() == null && this.getMediaType() != null)
        && !MediaType.ANY.matches(this.mimeType) && !MediaType.ANY.matches(that.mimeType)) {
      return false;
    }

    if (this.getMediaType() != null && !this.getMediaType().matches(that.getMediaType())
        && !MediaType.ANY.matches(that.getMediaType()) && !MediaType.ANY.matches(this.getMediaType())) {
      return false;
    }

    if (!fromPrimitive(this.getType()).isAssignableFrom(fromPrimitive(that.getType()))) {
      return false;
    }

    return true;
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

    if (!type.equals(that.type)) {
      return false;
    }

    // TODO MULE-9987 Fix this
    // ANY_MIME_TYPE will match to a null or non-null value for MediaType
    if ((this.mimeType == null && that.mimeType != null || that.mimeType == null && this.mimeType != null)
        && !MediaType.ANY.matches(that.mimeType)) {
      return false;
    }

    if (this.mimeType != null && !mimeType.matches(that.mimeType) && !MediaType.ANY.matches(that.mimeType)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    // TODO MULE-9987 Check if it is actually needed to leave the java type out.
    int result = type.hashCode();
    result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
    return result;
  }


  @Override
  public String toString() {
    return "SimpleDataType{" + "type=" + (type == null ? null : type.getName()) + ", mimeType='" + mimeType + '\'' + '}';
  }
}
