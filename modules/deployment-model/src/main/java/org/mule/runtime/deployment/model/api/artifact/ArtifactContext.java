/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;

/**
 * An artifact context contains all the information related to an {@link org.mule.runtime.module.artifact.Artifact} that contains
 * configuration.
 *
 * @since 4.0
 */
public interface ArtifactContext {

  /**
   * @return the artifact {@link MuleContext}
   */
  MuleContext getMuleContext();

  /**
   * @return a service to do connectivity testing over resources of the artifact.
   */
  ConnectivityTestingService getConnectivityTestingService();

  /**
   * @return the {@link MetadataService} for the current Artifact.
   * @see MetadataService
   */
  MetadataService getMetadataService();

  ValueProviderService getValueProviderService();
}
