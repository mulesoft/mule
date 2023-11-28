/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.System.getProperties;
import static org.mule.runtime.api.util.MuleSystemProperties.SINGLE_DEPLOYMENT_PROPERTY;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentService;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

public class DeploymentExecutor {

  private boolean artifactDeployed = false;
  private final DeploymentService deploymentService;

  public DeploymentExecutor(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  public <D extends DeployableArtifactDescriptor, T extends Artifact<D>> void deploy(final URI artifactArchiveUri,
                                                                                     final Optional<Properties> deploymentProperties,
                                                                                     File artifactDeploymentFolder,
                                                                                     ArchiveDeployer<D, T> archiveDeployer)
      throws IOException {
    if (!isSingleDeployment() || (isSingleDeployment() && !this.artifactDeployed)) {
      this.artifactDeployed = true;
      deployTemplateMethod(artifactArchiveUri, deploymentProperties, artifactDeploymentFolder, archiveDeployer);
    }
  }

  private <D extends DeployableArtifactDescriptor, T extends Artifact<D>> void deployTemplateMethod(final URI artifactArchiveUri,
                                                                                                    final Optional<Properties> deploymentProperties,
                                                                                                    File artifactDeploymentFolder,
                                                                                                    ArchiveDeployer<D, T> archiveDeployer)
      throws IOException {
    ((MuleDeploymentService) this.deploymentService).executeSynchronized(() -> {
      try {
        File artifactLocation = FileUtils.toFile(artifactArchiveUri.toURL());
        String fileName = artifactLocation.getName();
        if (fileName.endsWith(".jar")) {
          archiveDeployer.deployPackagedArtifact(artifactArchiveUri, deploymentProperties);
        } else {
          if (!artifactLocation.getParent().equals(artifactDeploymentFolder.getPath())) {
            try {
              FileUtils.copyDirectory(artifactLocation, new File(artifactDeploymentFolder, fileName));
            } catch (IOException e) {
              throw new MuleRuntimeException(e);
            }
          }
          archiveDeployer.deployExplodedArtifact(fileName, deploymentProperties);
        }
      } catch (MalformedURLException e) {
        throw new MuleRuntimeException(e);
      }
    });
  }

  private boolean isSingleDeployment() {
    return getProperties().containsKey(SINGLE_DEPLOYMENT_PROPERTY);
  }
}
