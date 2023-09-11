/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;

/**
 * Represents a container class loader.
 *
 * @since 4.5
 */
public interface MuleContainerClassLoaderWrapper {

  /**
   * @return the filtered class loader encapsulating the internals of the container class loader.
   */
  ArtifactClassLoader getContainerClassLoader();

  /**
   * @return the container class loader lookup policy.
   */
  ClassLoaderLookupPolicy getContainerClassLoaderLookupPolicy();
}
