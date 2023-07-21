/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.util.Collections.singletonList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

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
    checkArgument(classLoader != null, "classLoader cannot be null");

    classLoaders = singletonList(classLoader);
  }

  @Override
  public List<ClassLoader> getClassLoaders(ClassLoader classLoader) {
    return classLoaders;
  }
}
