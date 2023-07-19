/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
