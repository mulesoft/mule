/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api;

import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.artifact.Artifact;

import java.io.File;

/**
 * An Artifact is an abstract representation of a deployable unit within the mule container.
 *
 * @param <D> The type of the artifact's descriptor
 */
public interface DeployableArtifact<D extends DeployableArtifactDescriptor> extends Artifact<D> {

  /**
   * Install the artifact. Most commonly this includes the creation of the class loader and validation of resources.
   */
  void install() throws InstallException;

  /**
   * Initialise the artifact resources
   */
  void init();

  /**
   * Initialise the minimal resources required for this artifact to execute components.
   */
  void lazyInit();

  /**
   * Starts the artifact execution
   */
  void start() throws DeploymentStartException;

  /**
   * Stops the artifact execution
   */
  void stop();

  /**
   * Dispose the artifact. Most commonly this includes the release of the resources held by the artifact
   */
  void dispose();

  /**
   * @return MuleContext created from the artifact configurations files.
   */
  MuleContext getMuleContext();

  /**
   * @return the directory where the artifact content is stored.
   */
  File getLocation();

  /**
   * @return a service to test connection over configuration components.
   */
  ConnectivityTestingService getConnectivityTestingService();

  /**
   * @return the {@link MetadataService} which can resolve the metadata of the components inside
   * the current {@link DeployableArtifact}
   * @see MetadataService
   */
  MetadataService getMetadataService();
}
