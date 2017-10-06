/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;

/**
 * Deploys a file based artifact into the mule container.
 *
 * @param <T> type of the artifact to deploy
 */
public interface ArchiveDeployer<T extends Artifact> {

  /**
   * Indicates if a previously failed artifact (zombie) configuration was updated on the file system.
   *
   * @param artifactName name of the artifact to check. Non empty.
   * @return true if the zombie artifact was updated, false it the artifact is not a zombie or it was not updated.
   */
  boolean isUpdatedZombieArtifact(String artifactName);

  T deployPackagedArtifact(URI domainArchiveUrl, Optional<Properties> deploymentProperties) throws DeploymentException;

  T deployPackagedArtifact(String zip, Optional<Properties> deploymentProperties) throws DeploymentException;

  void undeployArtifact(String artifactId);

  File getDeploymentDirectory();

  void setDeploymentListener(CompositeDeploymentListener deploymentListener);

  void redeploy(T artifact, Optional<Properties> deploymentProperties) throws DeploymentException;

  Map<String, Map<URI, Long>> getArtifactsZombieMap();

  void setArtifactFactory(ArtifactFactory<T> artifactFactory);

  void undeployArtifactWithoutUninstall(T artifact);

  void deployArtifact(T artifact, Optional<Properties> deploymentProperties) throws DeploymentException;

  T deployExplodedArtifact(String artifactDir, Optional<Properties> deploymentProperties);
}
