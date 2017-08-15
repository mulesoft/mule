/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import java.util.Map;

/**
 * Defines which resources in a class loader should be looked up using parent-first, child-first or child only strategies.
 * <p/>
 * Default lookup strategy is parent first. To use child-first, the corresponding package must be added as an overridden. To use
 * child-only, the corresponding package must be added as blocked.
 */
public interface ClassLoaderLookupPolicy {

  /**
   * Returns the lookup strategy to use for a given class.
   *
   * @param className class to lookup.
   * @return the configured lookup strategy for the class's package or {@link ChildFirstLookupStrategy#CHILD_FIRST} if not
   *         explicit configuration was defined for the class's package.
   */
  LookupStrategy getClassLookupStrategy(String className);

  /**
   * Returns the lookup strategy to use for a given package.
   *
   * @param packageName package to lookup.
   * @return the configured lookup strategy for the class's package or {@link ChildFirstLookupStrategy#CHILD_FIRST} if not
   *         explicit configuration was defined for the package.
   */
  LookupStrategy getPackageLookupStrategy(String packageName);

  /**
   * Creates a new instance extending the original poclicy configuration
   *
   * @param lookupStrategies lookup strategies to use with specific packages. Non null.
   * @return a new policy containing the lookup strategies from the original policy and the lookup strategies passed on the
   *         parameter.
   */
  ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies);

}
