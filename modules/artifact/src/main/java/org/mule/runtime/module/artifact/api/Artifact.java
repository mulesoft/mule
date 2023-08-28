/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.File;

/**
 * An Artifact is an abstract representation of an isolated module within the mule container.
 *
 * @param <D> The type of the artifact's descriptor
 */
@NoImplement
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
