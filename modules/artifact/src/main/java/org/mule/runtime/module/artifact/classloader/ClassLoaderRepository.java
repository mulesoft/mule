/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

/**
 * Provides access to the {@link ClassLoader} registered on the container.
 */
public interface ClassLoaderRepository {

  /**
   * Returns a class loader with a given ID.
   *
   * @param classLoaderId identifies the class loader to find. Non empty.
   * @return the classloader registered under the given ID, null if no class loader with that ID is registered.
   */
  ClassLoader find(String classLoaderId);

  /**
   * Returns the ID for a given class loader
   *
   * @param classLoader instance for which the ID is searched for.
   * @return the class loader ID if the class loader is registered. Null otherwise
   */
  String getId(ClassLoader classLoader);
}
