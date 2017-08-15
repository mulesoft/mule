/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import java.util.Optional;

/**
 * Provides access to the {@link ClassLoader} registered on the container.
 */
public interface ClassLoaderRepository {

  /**
   * Returns a class loader with a given ID.
   *
   * @param classLoaderId identifies the class loader to find. Non empty.
   * @return an {@link Optional} {@link ClassLoader} for the provided ID.
   */
  Optional<ClassLoader> find(String classLoaderId);

  /**
   * Returns the ID for a given class loader
   *
   * @return an {@link Optional} {@link String} corresponding to the ID which is being searched for.
   */
  Optional<String> getId(ClassLoader classLoader);
}
