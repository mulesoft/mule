/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.valueOf;
import static java.util.Optional.of;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveArtifactStatusDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;

import org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a listener to persist stop events of Mule artifacts using deployment properties.
 */
final class ArtifactStoppedDeploymentPersistenceListener implements ArtifactStoppedPersistenceListener {

  private static transient final Logger logger = LoggerFactory.getLogger(ArtifactStoppedDeploymentPersistenceListener.class);
  /**
   * A possible race condition could happen if a stop request and a shutdown request are concurrently sent to mule, in order to
   * prevent it this property is defined as an AtomicBoolean.
   */
  private AtomicBoolean shouldPersist;
  private String artifactName;

  public ArtifactStoppedDeploymentPersistenceListener(String artifactName) {
    this.artifactName = artifactName;
    shouldPersist = new AtomicBoolean(true);
  }

  @Override
  public void onStart() {
    Properties properties = new Properties();
    properties.setProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, valueOf(true));
    try {
      resolveArtifactStatusDeploymentProperties(artifactName, of(properties));
    } catch (IOException e) {
      logger.error("ArtifactStoppedDeploymentPersistenceListener failed to process notification onStart for artifact "
          + artifactName, e);
    }
  }

  @Override
  public void onStop() {
    if (!shouldPersist.get()) {
      return;
    }
    Properties properties = new Properties();
    properties.setProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, valueOf(false));
    try {
      resolveArtifactStatusDeploymentProperties(artifactName, of(properties));
    } catch (IOException e) {
      logger.error("ArtifactStoppedDeploymentPersistenceListener failed to process notification onStop for artifact "
          + artifactName, e);
    }
  }

  @Override
  public void doNotPersist() {
    shouldPersist.set(false);
  }

  @Override
  public void deletePersistenceProperties() {
    try {
      resolveArtifactStatusDeploymentProperties(artifactName, of(new Properties()));
    } catch (IOException e) {
      logger.error("ArtifactStoppedDeploymentPersistenceListener failed to delete persistence for artifact {}",
                   artifactName, e);
    }
  }
}
