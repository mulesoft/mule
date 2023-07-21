/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a {@link LookupStrategy} that given a classloader, will search on the classloader's parent first, before searching on
 * the provided one.
 */
public class ParentFirstLookupStrategy implements LookupStrategy {

  /**
   * Provides access to the strategy
   */
  public static final LookupStrategy PARENT_FIRST = new ParentFirstLookupStrategy();

  private ParentFirstLookupStrategy() {}

  @Override
  public List<ClassLoader> getClassLoaders(ClassLoader classLoader) {
    List<ClassLoader> classLoaders = new ArrayList<>(2);
    if (classLoader.getParent() != null) {
      classLoaders.add(classLoader.getParent());
    }
    classLoaders.add(classLoader);
    return classLoaders;
  }
}
