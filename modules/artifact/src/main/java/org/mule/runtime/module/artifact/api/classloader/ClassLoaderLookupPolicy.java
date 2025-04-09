/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Defines which resources in a class loader should be looked up using parent-first, child-first or child only strategies.
 * <p/>
 * Default lookup strategy is parent first. To use child-first, the corresponding package must be added as an overridden. To use
 * child-only, the corresponding package must be added as blocked.
 */
@NoImplement
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
   * Creates a new instance extending the original policy configuration.
   * <p>
   * This is equivalent of calling {@link #extend(Map, boolean)} with {@code overwrite=false}.
   *
   * @param lookupStrategies lookup strategies to use with specific packages. Non null.
   * @return a new policy containing the lookup strategies from the original policy and the lookup strategies passed on the
   *         parameter.
   */
  ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies);

  /**
   * Creates a new instance extending the original policy configuration.
   * <p>
   * This is equivalent of calling {@link #extend(Stream, LookupStrategy, boolean)} with {@code overwrite=false}.
   *
   * @param packages       specific packages to set the lookupStrategy to. Non null.
   * @param lookupStrategy lookup strategies to use with the provided packages. Non null.
   * @return a new policy containing the lookup strategies from the original policy and the lookup strategies passed on the
   *         parameter.
   *
   * @since 4.5
   */
  ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy);

  /**
   * Creates a new instance extending the original policy configuration
   *
   * @param lookupStrategies lookup strategies to use with specific packages. Non null.
   * @param overwrite        if a lookupStrategy for a package provided in {@code lookupStrategies} already exists on this policy,
   *                         it will be overridden or not depending on this value.
   * @return a new policy containing the lookup strategies from the original policy and the lookup strategies passed on the
   *         parameter.
   */
  ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite);

  /**
   * Creates a new instance extending the original policy configuration.
   * <p>
   * This is equivalent of calling {@link #extend(Stream, LookupStrategy, boolean)} with {@code overwrite=false}.
   *
   * @param packages       specific packages to set the lookupStrategy to. Non null.
   * @param lookupStrategy lookup strategies to use with the provided packages. Non null.
   * @param overwrite      if a lookupStrategy for a package provided in {@code lookupStrategies} already exists on this policy,
   *                       it will be overridden or not depending on this value.
   * @return a new policy containing the lookup strategies from the original policy and the lookup strategies passed on the
   *         parameter.
   *
   * @since 4.5
   */
  ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy, boolean overwrite);

}
