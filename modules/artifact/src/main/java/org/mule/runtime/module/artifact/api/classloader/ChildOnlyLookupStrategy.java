/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static java.util.Collections.singletonList;

import java.util.List;

/**
 * Defines a {@link LookupStrategy} that given a classloader, will search on that classloader without
 * searching on the parent classloader.
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
