/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.plugin;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

import java.util.List;

/**
 * Repository that defines {@link ArtifactPlugin} bundled with the container
 * 
 * @since 4.0
 */
public interface ArtifactPluginRepository {

  /**
   * @return a non null List of {@link ArtifactPluginDescriptor} corresponding to application plugins already bundled with the
   *         container.
   * @throws ArtifactDescriptorCreateException if an error happens while building the descriptors from application plugins file.
   */
  List<ArtifactPluginDescriptor> getContainerArtifactPluginDescriptors();
}
