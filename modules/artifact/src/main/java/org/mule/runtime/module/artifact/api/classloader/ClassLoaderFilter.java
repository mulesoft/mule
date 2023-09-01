/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

/**
 * Determines if a given class or resource is exported in a plugin classloader
 */
@NoImplement
public interface ClassLoaderFilter {

  String OBJECT_SIMPLE_NAME = Object.class.getSimpleName();

  /**
   * Determines if a given name must be accepted or filtered.
   *
   * @param name class name to check. Non empty.
   * @return true if the class is exported, false otherwise
   */
  boolean exportsClass(String name);

  /**
   * Determines if a given package is exported.
   *
   * @param name package name to check. Non empty.
   * @return true if the package is exported, false otherwise
   */
  default boolean exportsPackage(String name) {
    return exportsClass(name + OBJECT_SIMPLE_NAME);
  }

  /**
   * Determines if a given resource must be accepted or filtered.
   *
   * @param name resource name to check. Non empty.
   * @return true if the resource is exported, false otherwise
   */
  boolean exportsResource(String name);
}
