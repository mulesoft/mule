/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

/**
 * Indicates that an artifact is the owner of a region where other artifact are included as members.
 */
@NoImplement
public interface RegionOwnerArtifact {

  /**
   * @return the {@link RegionClassLoader} that represents the region that is owned by an artifact
   * @throws IllegalStateException if the artifact's class loader does not represent a region
   */
  RegionClassLoader getRegionClassLoader();
}
