/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

/**
 * Defines a {@link LookupStrategy} that will search on the container's classloader only, without searching on the given
 * classloader.
 */
public class ContainerOnlyLookupStrategy implements LookupStrategy {

  private final List<ClassLoader> classLoaders;

  /**
   * Creates a new instance
   *
   * @param containerClassLoader class loader containing the container classes. Not null.
   */
  public ContainerOnlyLookupStrategy(ClassLoader containerClassLoader) {
    checkArgument(containerClassLoader != null, "containerClassLoader cannot be null");
    classLoaders = new ArrayList<>();
    classLoaders.add(containerClassLoader);
  }

  @Override
  public List<ClassLoader> getClassLoaders(ClassLoader classLoader) {
    return classLoaders;
  }
}
