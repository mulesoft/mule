/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import java.util.Set;

/**
 * This interface represents that a class can have classloaders attached, and they can be retrieved.
 */
public interface WithAttachedClassLoaders {

  /**
   * Registers a classloader.
   *
   * @param classLoader the {@link ClassLoader} to be registered.
   */
  void attachClassLoader(ClassLoader classLoader);

  /**
   * @return a {@link Set} containing all the registered {@link ClassLoader} instances.
   */
  Set<ClassLoader> getAttachedClassLoaders();
}
