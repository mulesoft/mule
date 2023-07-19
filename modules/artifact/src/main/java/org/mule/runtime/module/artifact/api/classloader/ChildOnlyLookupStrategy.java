/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.util.Collections.singletonList;

import java.util.List;

/**
 * Defines a {@link LookupStrategy} that given a classloader, will search on that classloader without searching on the parent
 * classloader.
 */
public class ChildOnlyLookupStrategy implements LookupStrategy {

  /**
   * Provides access to the strategy
   */
  public static final LookupStrategy CHILD_ONLY = new ChildOnlyLookupStrategy();

  private ChildOnlyLookupStrategy() {}

  @Override
  public List<ClassLoader> getClassLoaders(ClassLoader classLoader) {
    return singletonList(classLoader);
  }
}
