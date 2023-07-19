/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
