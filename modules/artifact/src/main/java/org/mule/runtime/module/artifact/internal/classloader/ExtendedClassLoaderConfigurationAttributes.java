/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.internal.classloader;

import java.util.Map;

import org.apache.commons.collections.map.AbstractMapDecorator;

/**
 * Allows to extends the attributes defined for a {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration} when it
 * is being loaded by {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader} without changing the loader's
 * API.
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
