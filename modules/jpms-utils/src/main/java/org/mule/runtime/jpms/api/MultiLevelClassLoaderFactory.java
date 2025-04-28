/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Defines a way for creating a multi-level {@link ClassLoader}.
 *
 * @since 4.6
 */
public interface MultiLevelClassLoaderFactory {

  /**
   * An implementation of {@link MultiLevelClassLoaderFactory} that creates {@link URLClassLoader} for the levels.
   */
  final MultiLevelClassLoaderFactory MULTI_LEVEL_URL_CLASSLOADER_FACTORY =
      (parent, modulePathEntriesParent, modulePathEntriesChild) -> new URLClassLoader(modulePathEntriesChild,
                                                                                      new URLClassLoader(modulePathEntriesParent,
                                                                                                         parent));

  /**
   * Creates a multi-level {@link ClassLoader} for the provided parameters.
   * <p>
   * The resulting structure must be:
   * <p>
   * {@code "parent" classloader <- "intermediate" classloader <- "child" classlaoder}
   * <p>
   * Arrows represent the {@code parent} relationship.
   *
   * @param parent              the {@link ClassLoader} to use as parent of the intermediate one.
   * @param intermediateEntries the entries that the intermediate {@link ClassLoader} will contain.
   * @param childEntries        the entries that the child {@link ClassLoader} will contain.
   * @return the child classloader
   */
  ClassLoader create(ClassLoader parent, URL[] intermediateEntries, URL[] childEntries);
}
