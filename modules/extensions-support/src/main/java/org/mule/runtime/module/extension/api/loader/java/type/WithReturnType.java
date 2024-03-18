/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
