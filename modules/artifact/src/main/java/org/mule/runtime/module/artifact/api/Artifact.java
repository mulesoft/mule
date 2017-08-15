/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.File;

/**
 * An Artifact is an abstract representation of an isolated module within the mule container.
 *
 * @param <D> The type of the artifact's descriptor
 */
public interface Artifact<D extends ArtifactDescriptor> {

  /**
   * @return the artifact name
   */
  String getArtifactName();

  /**
   * @return the artifact unique identifier inside the container. Non empty.
   */
  String getArtifactId();

  /**
   * @return the artifact descriptor
   */
  D getDescriptor();

  /**
   * @return an array with the configuration files of the artifact. Never returns null. If there's no configuration file then
   *         returns an empty array.
   */
  File[] getResourceFiles();

  /**
   * @return class loader responsible for loading resources for this artifact.
   */
  ArtifactClassLoader getArtifactClassLoader();
}
