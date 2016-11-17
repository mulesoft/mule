/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.artifact.Artifact;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * Deploys a file based artifact into the mule container.
 *
 * @param <T> type of the artifact to deploy
 */
public interface ArchiveDeployer<T extends Artifact> {

  T deployPackagedArtifact(String zip) throws DeploymentException;

  T deployExplodedArtifact(String artifactDir) throws DeploymentException;

  /**
   * Indicates if a previously failed artifact (zombie) configuration was updated on the file system.
   *
   * @param artifactName name of the artifact to check. Non empty.
   * @return true if the zombie artifact was updated, false it the artifact is not a zombie or it was not updated.
   */
  boolean isUpdatedZombieArtifact(String artifactName);

  T deployPackagedArtifact(URL artifactAchivedUrl);

  void undeployArtifact(String artifactId);

  File getDeploymentDirectory();

  void setDeploymentListener(CompositeDeploymentListener deploymentListener);

  void redeploy(T artifact) throws DeploymentException;

  Map<URL, Long> getArtifactsZombieMap();

  void setArtifactFactory(ArtifactFactory<T> artifactFactory);

  void undeployArtifactWithoutUninstall(T artifact);

  void deployArtifact(T artifact) throws DeploymentException;
}
