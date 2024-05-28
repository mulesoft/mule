/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import org.mule.api.annotation.NoInstantiate;

import java.util.List;

/**
 * Defines a {@link LookupStrategy} that will search on a given classloader only, without searching on the current classloader.
 *
 * @since 4.0
 */
@NoInstantiate
public final class DelegateOnlyLookupStrategy implements LookupStrategy {

  private final List<ClassLoader> classLoaders;

  /**
   * Creates a new instance
   *
   * @param classLoader class loader containing the container classes. Not null.
   */
  public DelegateOnlyLookupStrategy(ClassLoader classLoader) {
    requireNonNull(classLoader, "classLoader cannot be null");

    classLoaders = singletonList(classLoader);
  }

  @Override
  public List<ClassLoader> getClassLoaders(ClassLoader classLoader) {
    return classLoaders;
  }

  @Override
  public String toString() {
    return "DelegateOnlyLookupStrategy[classloaders=" + classLoaders + "]";
  }
}
