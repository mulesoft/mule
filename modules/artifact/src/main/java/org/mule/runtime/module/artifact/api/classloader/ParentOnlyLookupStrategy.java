/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a {@link LookupStrategy} that given a classloader, will search on that classloader's parent
 * without searching on the provided classloader.
 */
public class ParentOnlyLookupStrategy implements LookupStrategy {

  /**
   * Provides access to the strategy
   */
  public static final LookupStrategy PARENT_ONLY = new ParentOnlyLookupStrategy();

  private ParentOnlyLookupStrategy() {}

  @Override
  public List<ClassLoader> getClassLoaders(ClassLoader classLoader) {
    List<ClassLoader> classLoaders = new ArrayList<>(1);
    if (classLoader.getParent() != null) {
      classLoaders.add(classLoader.getParent());
    }
    return classLoaders;
  }
}
