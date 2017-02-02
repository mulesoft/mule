/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

/**
 * A generic contract for any kind of component that could be of certain type
 *
 * @since 4.0
 */
interface WithType {

  /**
   * @return The {@link TypeWrapper} of the represented component
   */
  Type getType();

  /**
   * @param typeLoader {@link ClassTypeLoader} that will load the {@link Class} and represent it as a {@link MetadataType}
   * @return The {@link MetadataType} representation of the component type
   */
  default MetadataType getMetadataType(ClassTypeLoader typeLoader) {
    return typeLoader.load(getType().getDeclaringClass());
  }
}
