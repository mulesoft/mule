/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

import org.mule.runtime.module.artifact.Artifact;

import java.io.File;
import java.io.IOException;

/**
 * Generic Factory for an {@link Artifact}.
 */
public interface ArtifactFactory<T extends Artifact> {

  /**
   * Creates an Artifact
   *
   * @param artifactDir directory where the artifact is located
   * @return the newly created Artifact
   */
  T createArtifact(File artifactDir) throws IOException;

  /**
   * @return the directory of the Artifact. Usually this directory contains the Artifact resources
   */
  File getArtifactDir();

}
