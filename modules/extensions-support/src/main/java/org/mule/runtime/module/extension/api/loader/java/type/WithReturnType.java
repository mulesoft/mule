/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.model.MetadataType;

/**
 * A generic contract for any kind of component that could contain a return type
 *
 * @since 4.0
 */
@NoImplement
interface WithReturnType {

  /**
   * @return the return type {@link Type} of the implementer component
   */
  Type getReturnType();

  /**
   * @return The {@link MetadataType} of the return type of the current element
   * @since 4.1
   */
  MetadataType getReturnMetadataType();

  /**
   * @return The {@link MetadataType} of the attributes type of the current element
   * @since 4.1
   */
  MetadataType getAttributesMetadataType();
}
