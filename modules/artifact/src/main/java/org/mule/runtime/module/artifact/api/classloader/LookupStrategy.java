/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

import java.util.List;

/**
 * Defines how a given class should be searched based on a given {@link ClassLoader}
 */
@NoImplement
public interface LookupStrategy {

  /**
   * Determines which class loaders must be used to search a class
   *
   * @param classLoader class loader form which the class is being looked up. Non null
   * @return a non null list of class loaders that must be searched on following the assigned order.
   */
  List<ClassLoader> getClassLoaders(ClassLoader classLoader);

}
