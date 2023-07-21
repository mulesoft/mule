/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.api;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderManager;

/**
 * Enables injection of container's {@link ArtifactClassLoaderManager}.
 *
 * @deprecated on 4.1, use @Inject on a field or setter method of type {@link ArtifactClassLoaderManager}
 */
@Deprecated
public interface ArtifactClassLoaderManagerAware {

  /**
   * Sets the artifact classloader
   *
   * @param artifactClassLoaderManager manager used to register created artifact class loaders. Non null.
   */
  void setArtifactClassLoaderManager(ArtifactClassLoaderManager artifactClassLoaderManager);
}
