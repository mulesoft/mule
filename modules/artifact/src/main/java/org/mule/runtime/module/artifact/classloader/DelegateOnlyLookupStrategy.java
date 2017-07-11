/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.util.Collections.singletonList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.util.List;

/**
 * Defines a {@link LookupStrategy} that will search on a given classloader only, without searching
 * on the current classloader.
 *
 * @since 4.0
 */
public class DelegateOnlyLookupStrategy implements LookupStrategy {

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
