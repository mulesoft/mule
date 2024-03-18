/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

import java.util.Optional;

/**
 * A generic contract for any kind of component which is derived from a declaration inside a {@link Class}
 *
 * @since 4.0
 */
@NoImplement
public interface WithDeclaringClass {

  /**
   * @return the class that this {@link Type} represents
   */
  Optional<Class<?>> getDeclaringClass();
}
