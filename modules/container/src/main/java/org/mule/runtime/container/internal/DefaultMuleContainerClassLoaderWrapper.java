/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;

/**
 * Default implementation of {@link MuleContainerClassLoaderWrapper}.
 *
 * @since 4.5
 */
public class DefaultMuleContainerClassLoaderWrapper implements MuleContainerClassLoaderWrapper {

  private final ArtifactClassLoader containerClassLoader;

  public DefaultMuleContainerClassLoaderWrapper(ArtifactClassLoader containerClassLoader) {
    this.containerClassLoader = containerClassLoader;
  }

  @Override
  public ArtifactClassLoader getContainerClassLoader() {
    return containerClassLoader;
  }

  @Override
  public ClassLoaderLookupPolicy getContainerClassLoaderLookupPolicy() {
    return containerClassLoader.getClassLoaderLookupPolicy();
  }
}
