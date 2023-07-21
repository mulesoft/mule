/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a {@link LookupStrategy} that given a classloader, will search on that classloader first and then on it's father
 */
public class ChildFirstLookupStrategy implements LookupStrategy {

  /**
   * Provides access to the strategy
   */
  public static final LookupStrategy CHILD_FIRST = new ChildFirstLookupStrategy();

  private ChildFirstLookupStrategy() {}

  @Override
  public List<ClassLoader> getClassLoaders(ClassLoader classLoader) {
    List<ClassLoader> classLoaders = new ArrayList<>(2);
    classLoaders.add(classLoader);
    if (classLoader.getParent() != null) {
      classLoaders.add(classLoader.getParent());
    }
    return classLoaders;
  }
}
