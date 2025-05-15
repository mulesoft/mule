/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import com.github.benmanes.caffeine.cache.LoadingCache;

import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;

/**
 * Provides utilities for creating multi-parent/composite classloaders.
 *
 * @since 4.5
 */
public class MultiParentClassLoaderUtils {

  private static final ClassLoader CONTAINER_CLASS_LOADER = MultiParentClassLoaderUtils.class.getClassLoader();

  private MultiParentClassLoaderUtils() {
    // nothing to do
  }

  // This only works because the cache uses an identity hashCode() and equals() for keys when they are configured as weak.
  // (check com.github.benmanes.caffeine.cache.Caffeine.weakKeys javadoc).
  // If that is not the case, this will never work because we want to compare class loaders by instance.
  // The idea for this cache is to avoid the creation of multiple CompositeClassLoader instances with the same delegates.
  // That is because ByteBuddy uses the composite class loader to define the enhanced class and every new instance would load
  // the same defined class over and over again, causing metaspace OOM in some scenarios.
  private static final LoadingCache<ClassLoader, ClassLoader> COMPOSITE_CL_CACHE = newBuilder()
      .weakKeys()
      .weakValues()
      .build(cl -> new MultipleParentClassLoader.Builder(false)
          .append(CONTAINER_CLASS_LOADER, cl)
          .build());

  /**
   * Creates a new classloader that has visibility on the Mule Container classloader and {@code deploymentClassLoader}.
   * <p>
   * If {@code deploymentClassLoader} is the container classlaoder, the container classlaoder will be returned.
   * <p>
   * If this is called more than once the same classloader, the same instance will be returned.
   *
   * @param deploymentClassLoader a classloader that has visibility on classes specific for a deployment.
   * @return a classloader with visibility on the Mule Container classloader and {@code deploymentClassLoader}.
   */
  public static ClassLoader multiParentClassLoaderFor(ClassLoader deploymentClassLoader) {
    if (deploymentClassLoader == null) {
      return CONTAINER_CLASS_LOADER;
    } else {
      return COMPOSITE_CL_CACHE.get(deploymentClassLoader);
    }
  }

}
