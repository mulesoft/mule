/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

/**
 * Indicates that an artifact is the owner of a region where other artifact are included as members.
 */
public interface RegionOwnerArtifact {

  /**
   * @return the {@link RegionClassLoader} that represents the region that is owned by an artifact
   * @throws IllegalStateException if the artifact's class loader does not represent a region
   */
  RegionClassLoader getRegionClassLoader();
}
