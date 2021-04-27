/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.deployment.model.api.DeployableArtifact;

/**
 * Deploys and Undeploys artifacts in the container.
 *
 * @param <T> artifact type
 */
public interface ArtifactDeployer<T extends DeployableArtifact> {

  /**
   * Deploys an artifact.
   * <p>
   * The deployer executes the artifact installation phases until the artifact is deployed. After this method call the Artifact
   * will be installed in the container and its start dispatched asynchronously.
   *
   * @param artifact      artifact to be deployed
   * @param startArtifact whether the artifact should be started after initialisation
   */
  void deploy(final T artifact, boolean startArtifact);

  /**
   * Deploys an artifact.
   * <p>
   * The deployer executes the artifact installation phases until the artifact is deployed. After this method call the Artifact
   * will be installed in the container and its start dispatched asynchronously.
   *
   * @param artifact artifact to be deployed
   */
  default void deploy(final T artifact) {
    deploy(artifact, true);
  }

  /**
   * Undeploys an artifact.
   * <p>
   * The deployer executes the artifact desinstallation phases until de artifact is undeployed. After this method call the
   * Artifact will not longer be running inside the container.
   *
   * @param artifact artifact to be undeployed
   */
  void undeploy(final T artifact);

  /**
   * Cancels the persistence of a stop of an artifact.
   * <p>
   * A stop of a certain artifact must only be persisted when it was stopped by the external users. In case of undeployment, it
   * should not be persisted.
   *
   * @param artifact artifact to be undeployed
   */
  void doNotPersistArtifactStop(T artifact);

  /**
   * Cancels the persistence of a flow of an app.
   * <p>
   * A stop of a certain flow must only be persisted when it was stopped by the external users. In case of undeployment, it should
   * not be persisted.
   *
   * @param artifactName name of the artifact to be undeployed
   */
  void doNotPersistFlowsStop(String artifactName);

}
