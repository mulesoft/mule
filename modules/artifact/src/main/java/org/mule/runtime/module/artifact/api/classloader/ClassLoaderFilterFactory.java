/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

/**
 * Creates {@link ClassLoaderFilter} instances
 */
public interface ClassLoaderFilterFactory {

  /**
   * Creates a filter based on the provided configuration
   *
   * @param exportedClassPackages comma separated list of class packages to export. Can be null
   * @param exportedResources comma separated list of resources to export. Can be null
   * @return a class loader filter that matches the provided configuration
   */
  ArtifactClassLoaderFilter create(String exportedClassPackages, String exportedResources);
}
