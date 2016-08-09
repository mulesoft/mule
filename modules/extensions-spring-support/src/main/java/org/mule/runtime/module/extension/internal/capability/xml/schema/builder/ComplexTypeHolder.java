/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexType;

/**
 * Container class to relate a {@link ComplexType} with its associated {@link MetadataType}
 *
 * @since 4.0
 */
final class ComplexTypeHolder {

  private final ComplexType complexType;
  private final MetadataType type;

  ComplexTypeHolder(ComplexType complexType, MetadataType type) {
    this.complexType = complexType;
    this.type = type;
  }

  public ComplexType getComplexType() {
    return complexType;
  }

  public MetadataType getType() {
    return type;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ComplexTypeHolder) {
      ComplexTypeHolder other = (ComplexTypeHolder) obj;
      return type.equals(other.getType());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }
}
