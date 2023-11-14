/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Set;

/**
 * No-op implementation of JpmsUtils to use when running on JVM 8.
 * 
 * @since 4.5
 */
public final class JpmsUtils {

  private JpmsUtils() {
    // Nothing to do
  }

  /**
   * Creates a basic classLoader containing the given {@code modulePathEntries} and with the given {@code parent}.
   * 
   * @param modulePathEntries the URLs from which to load classes and resources
   * @param parent            the parent class loader for delegation
   * @return a new classLoader.
   */
  public static ClassLoader createModuleLayerClassLoader(URL[] modulePathEntries, ClassLoader parent) {
    return new URLClassLoader(modulePathEntries, parent);
  }

  /**
   * Creates two classLoaders for the given {@code modulePathEntriesParent} and {@code modulePathEntriesChild} and with the given
   * {@code parent}.
   * 
   * @param modulePathEntriesParent the URLs from which to find the modules of the parent
   * @param modulePathEntriesChild  the URLs from which to find the modules of the child
   * @param childClassLoaderFactory how the classLoader for the child is created
   * @param parent                  the parent class loader for delegation
   * @return a new classLoader.
   */
  public static ClassLoader createModuleLayerClassLoader(URL[] modulePathEntriesParent, URL[] modulePathEntriesChild,
                                                         MultiLevelClassLoaderFactory childClassLoaderFactory,
                                                         ClassLoader parent) {
    return childClassLoaderFactory.create(parent, modulePathEntriesParent, modulePathEntriesChild);
  }

  /**
   * Creates two classLoaders for the given {@code modulePathEntriesParent} and {@code modulePathEntriesChild}, with the layer
   * from the given {@code clazz} as parent, if any, and with the given {@code parentClassLoader}. A classLoader from which the
   * child modules can be read is returned.
   *
   * @param modulePathEntriesParent the URLs from which to find the modules of the parent
   * @param modulePathEntriesChild  the URLs from which to find the modules of the child
   * @param childClassLoaderFactory how the classLoader for the child is created, if moduleLayers are not used
   * @param parentClassLoader       the parent class loader for delegation
   * @param clazz                   the class from which to get the parent layer.
   * @return a new classLoader.
   */
  public static ClassLoader createModuleLayerClassLoader(URL[] modulePathEntriesParent, URL[] modulePathEntriesChild,
                                                         MultiLevelClassLoaderFactory childClassLoaderFactory,
                                                         ClassLoader parentClassLoader,
                                                         Optional<Class> clazz) {
    return childClassLoaderFactory.create(parentClassLoader, modulePathEntriesParent, modulePathEntriesChild);
  }

  public static void exploreJdkModules(Set<String> packages) {
    // nothing to do
  }

  public static void validateNoBootModuleLayerTweaking() {
    // nothing to do
  }

}
