/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import org.mule.runtime.core.api.config.bootstrap.ArtifactType;

/**
 * Loads the {@link BundleDescriptor} for Mule artifacts.
 * <p/>
 * Explicitly defined to enable definition of implementations using SPI.
 */
public interface BundleDescriptorLoader extends DescriptorLoader<BundleDescriptor> {

  default boolean supportsArtifactType(ArtifactType artifactType) {
    return true;
  }

}
