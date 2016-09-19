/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.deployment.model.api.DeployableArtifact;

/**
 * Deployes and Undeploys artifacts in the container.
 *
 * @param <T> artifact type
 */
public interface ArtifactDeployer<T extends DeployableArtifact> {

  /**
   * Deploys an artifact.
   *
   * The deployer executes the artifact installation phases until the artifact is deployed After this method call the Artifact
   * will be installed in the container and started.
   *
   * @param artifact artifact to be deployed
   */
  void deploy(final T artifact);

  /**
   * Undeploys an artifact.
   *
   * The deployer executes the artifact desinstallation phases until de artifact is undeployed. After this method call the
   * Artifact will not longer be running inside the container.
   *
   * @param artifact artifact to be undeployed
   */
  void undeploy(final T artifact);

}
