/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

/**
 * Determines if a given class or resource is exported in a plugin classloader
 */
public interface ClassLoaderFilter {

  /**
   * Determines if a given name must be accepted or filtered.
   *
   * @param name class name to check. Non empty.
   * @return true if the class is exported, false otherwise
   */
  boolean exportsClass(String name);

  /**
   * Determines if a given resource must be accepted or filtered.
   *
   * @param name resource name to check. Non empty.
   * @return true if the resource is exported, false otherwise
   */
  boolean exportsResource(String name);
}
