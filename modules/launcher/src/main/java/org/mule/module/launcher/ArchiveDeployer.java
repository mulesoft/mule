/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.artifact.ArtifactFactory;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * Deploys a file based artifact into the mule container.
 *
 * @param <T> type of the artifact to deploy
 */
public interface ArchiveDeployer<T extends Artifact>
{

    T deployPackagedArtifact(String zip) throws DeploymentException;

    T deployExplodedArtifact(String artifactDir) throws DeploymentException;

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
