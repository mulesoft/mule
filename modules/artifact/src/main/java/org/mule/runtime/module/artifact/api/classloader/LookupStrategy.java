/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import java.util.List;

/**
 * Defines how a given class should be searched based on a given {@link ClassLoader}
 */
public interface LookupStrategy {

  /**
   * Determines which class loaders must be used to search a class
   *
   * @param classLoader class loader form which the class is being looked up. Non null
   * @return a non null list of class loaders that must be searched on following the assigned order.
   */
  List<ClassLoader> getClassLoaders(ClassLoader classLoader);

}
