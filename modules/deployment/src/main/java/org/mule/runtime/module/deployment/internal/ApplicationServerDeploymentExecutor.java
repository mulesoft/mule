/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

/**
 * Is the default deploy action of a DeploymentService.
 *
 * @since 4.7.0
 */
public class ApplicationServerDeploymentExecutor extends DeploymentExecutorUtils implements DeploymentExecutor {

  private DeploymentService deploymentService;

  @Override
  public void setDeploymentService(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @Override
  public <D extends DeployableArtifactDescriptor, T extends Artifact<D>> void deploy(URI artifactArchiveUri,
                                                                                     Optional<Properties> deploymentProperties,
                                                                                     File artifactDeploymentFolder,
                                                                                     ArchiveDeployer<D, T> archiveDeployer)
      throws IOException {
    deployTemplateMethod(this.deploymentService, artifactArchiveUri, deploymentProperties, artifactDeploymentFolder,
                         archiveDeployer);
  }
}
