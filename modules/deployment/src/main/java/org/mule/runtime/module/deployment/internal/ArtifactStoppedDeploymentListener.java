/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;

public class ArtifactStoppedDeploymentListener implements DeploymentListener {

  private transient final Logger logger = LoggerFactory.getLogger(getClass());
  private AtomicBoolean shouldPersist;
  private String artifactName;

  public ArtifactStoppedDeploymentListener(String artifactName) {
    this.artifactName = artifactName;
    shouldPersist = new AtomicBoolean(true);
  }

  @Override
  public void onArtifactStarted(String artifactName, Registry registry) {
    Properties properties = new Properties();
    properties.setProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, String.valueOf(true));
    try {
      resolveDeploymentProperties(artifactName, Optional.of(properties));
    } catch (IOException e) {
      logger.error("ApplicationStoppedDeploymentListener failed to process notification onArtifactStopped for artifact "
          + artifactName, e);
    }
  }

  @Override
  public void onArtifactStopped(String artifactName, Registry registry) {
    if (!shouldPersist.get()) {
      return;
    }
    Properties properties = new Properties();
    properties.setProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, String.valueOf(false));
    try {
      resolveDeploymentProperties(artifactName, Optional.of(properties));
    } catch (IOException e) {
      logger.error("ApplicationStoppedDeploymentListener failed to process notification onArtifactStopped for artifact "
          + artifactName, e);
    }
  }

  public void onStopDoNotPersist() {
    shouldPersist.set(false);
  }

  public String getArtifactName() {
    return this.artifactName;
  }
}
