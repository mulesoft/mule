/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
