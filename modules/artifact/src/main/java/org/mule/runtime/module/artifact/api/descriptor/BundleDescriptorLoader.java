/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
