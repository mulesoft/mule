/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;

import org.mule.runtime.module.artifactapi.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifactapi.classloader.LookupStrategy;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Helper {@link ClassLoaderLookupPolicy} which uses the same {@link LookupStrategy} for all classes and packages.
 * <p>
 * Does not support the {@link ClassLoaderLookupPolicy#extend} methods.
 * <p>
 * Used in testing.
 */
class SimpleClassLoaderLookupPolicy implements ClassLoaderLookupPolicy {

  public static ClassLoaderLookupPolicy CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY = new SimpleClassLoaderLookupPolicy(CHILD_FIRST);
  public static ClassLoaderLookupPolicy PARENT_FIRST_CLASSLOADER_LOOKUP_POLICY = new SimpleClassLoaderLookupPolicy(PARENT_FIRST);

  private final LookupStrategy lookupStrategy;

  private SimpleClassLoaderLookupPolicy(LookupStrategy lookupStrategy) {
    this.lookupStrategy = lookupStrategy;
  }

  @Override
  public LookupStrategy getClassLookupStrategy(String className) {
    return lookupStrategy;
  }

  @Override
  public LookupStrategy getPackageLookupStrategy(String packageName) {
    return lookupStrategy;
  }

  @Override
  public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
    return null;
  }

  @Override
  public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy) {
    return null;
  }

  @Override
  public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite) {
    return null;
  }

  @Override
  public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy, boolean overwrite) {
    return null;
  }
}
