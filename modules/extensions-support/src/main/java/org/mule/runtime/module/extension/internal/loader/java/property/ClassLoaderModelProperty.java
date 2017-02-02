/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Links an {@link EnrichableModel} with a {@link ClassLoader}.
 *
 * @since 4.0
 */
public class ClassLoaderModelProperty implements ModelProperty {

  private final ClassLoader classLoader;

  /**
   * Creates a new instance
   *
   * @param classLoader the {@link ClassLoader} that {@code this} instance references
   */
  public ClassLoaderModelProperty(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * @return {@code classLoader}
   */
  @Override
  public String getName() {
    return "classLoader";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * @return The referenced {@link ClassLoader}
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }
}
