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

public class DeploymentExecutorSingleApp extends DeploymentExecutorUtils implements DeploymentExecutor {

  private DeploymentService deploymentService;

  @Override
  public final void setDeploymentService(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @Override
  public <D extends DeployableArtifactDescriptor, T extends Artifact<D>> void deploy(final URI artifactArchiveUri,
                                                                                     final Optional<Properties> deploymentProperties,
                                                                                     File artifactDeploymentFolder,
                                                                                     ArchiveDeployer<D, T> archiveDeployer)
      throws IOException {
    if (this.deploymentService.getApplications().isEmpty()) {
      deployTemplateMethod(this.deploymentService, artifactArchiveUri, deploymentProperties, artifactDeploymentFolder,
                           archiveDeployer);
    }
  }
}
