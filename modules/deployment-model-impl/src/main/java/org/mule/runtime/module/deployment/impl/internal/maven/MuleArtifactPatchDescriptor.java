/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.util.List;

public class MuleArtifactPatchDescriptor {

  private ArtifactCoordinates artifactCoordinates;
  private List<MuleArtifactPatch> patches;

  public ArtifactCoordinates getArtifactCoordinates() {
    return artifactCoordinates;
  }

  public void setArtifactCoordinates(ArtifactCoordinates artifactCoordinates) {
    this.artifactCoordinates = artifactCoordinates;
  }

  public List<MuleArtifactPatch> getPatches() {
    return patches;
  }

  public void setPatches(List<MuleArtifactPatch> patches) {
    this.patches = patches;
  }
}
