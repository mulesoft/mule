/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import java.util.Map;

import org.apache.commons.collections4.map.AbstractMapDecorator;

/**
 * Allows to extends the attributes defined for a {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration}
 * when it is being loaded by {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader} without
 * changing the loader's API.
 *
 * @since 4.2.0
 */
public abstract class ExtendedClassLoaderConfigurationAttributes extends AbstractMapDecorator {

  /**
   * Creates an instance of this extended attributes for the given descriptor.
   *
   * @param originalAttributes the original {@link Map} of attributes. Not null.
   */
  public ExtendedClassLoaderConfigurationAttributes(Map originalAttributes) {
    super(originalAttributes);
  }

}
