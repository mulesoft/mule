/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.custom.CustomizationService;

/**
 * Defines a listener for deployment events for Mule applications.
 */
public interface DeploymentListener {

  /**
   * Notifies that a deploy for a given application has started.
   *
   * @param artifactName the name of the application being deployed
   */
  default void onDeploymentStart(String artifactName) {

  }

  /**
   * Notifies that a deploy for a given application has successfully finished.
   *
   * @param artifactName the name of the application being deployed
   */
  default void onDeploymentSuccess(String artifactName) {

  }

  /**
   * Notifies that a deploy for a given application has finished with a failure.
   *
   * @param artifactName the name of the application being deployed
   * @param cause the cause of the failure
   */
  default void onDeploymentFailure(String artifactName, Throwable cause) {

  }

  /**
   * Notifies that an un-deployment for a given application has started.
   *
   * @param artifactName the name of the application being un-deployed
   */
  default void onUndeploymentStart(String artifactName) {

  }

  /**
   * Notifies that an un-deployment for a given application has successfully finished.
   *
   * @param artifactName the name of the application being un-deployed
   */
  default void onUndeploymentSuccess(String artifactName) {

  }

  /**
   * Notifies that an un-deployment for a given application has finished with a failure.
   *
   * @param artifactName the name of the application being un-deployed
   * @param cause the cause of the failure
   */
  default void onUndeploymentFailure(String artifactName, Throwable cause) {

  }

  /**
   * Notifies the artifact creation for a given app.
   *
   * @param artifactName name of the application that owns the mule context
   * @param customizationService customization service for server plugins
   */
  default void onArtifactCreated(String artifactName, CustomizationService customizationService) {

  }

  /**
   * Notifies the artifact initialisation for a given app.
   *
   * @param artifactName name of the application that owns the mule context
   * @param registry mule registry for the application being deployed
   */
  default void onArtifactInitialised(String artifactName, Registry registry) {

  }

  /**
   * Notifies artifact start for a given app.
   *
   * @param artifactName name of the application that owns the mule context
   * @param registry mule registry for the application being deployed
   */
  default void onArtifactStarted(String artifactName, Registry registry) {

  }

  /**
   * Notifies artifact stop for a given app.
   *
   * @param artifactName name of the application that owns the mule context
   * @param registry mule registry for the application being deployed
   */
  default void onArtifactStopped(String artifactName, Registry registry) {

  }
}
