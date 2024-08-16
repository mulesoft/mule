/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.module.deployment.internal.DeploymentListenerAdapter;

/**
 * Defines a listener for deployment events for all Mule deployable artifacts. Use {@link DeploymentListenerAdapter} to make this
 * listener work as a {@link DeploymentListener}
 *
 * @since 4.0
 */
public interface ArtifactDeploymentListener {

  /**
   * Notifies that a deploy for a given artifact has started.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being deployed
   */
  default void onDeploymentStart(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName) {
    onDeploymentStart(type.getArtifactType(), artifactName);
  }

  /**
   * Notifies that a deploy for a given artifact has successfully finished.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being deployed
   */
  default void onDeploymentSuccess(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName) {
    onDeploymentSuccess(type.getArtifactType(), artifactName);
  }

  /**
   * Notifies that a deploy for a given artifact has finished with a failure.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being deployed
   * @param cause        the cause of the failure
   */
  default void onDeploymentFailure(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName,
                                   Throwable cause) {
    onDeploymentFailure(type.getArtifactType(), artifactName, cause);
  }

  /**
   * Notifies that an un-deployment for a given artifact has started.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being un-deployed
   * @param type         the artifact type
   */
  default void onUndeploymentStart(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName) {
    onUndeploymentStart(type.getArtifactType(), artifactName);
  }

  /**
   * Notifies that an un-deployment for a given artifact has successfully finished.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being un-deployed
   */
  default void onUndeploymentSuccess(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName) {
    onUndeploymentSuccess(type.getArtifactType(), artifactName);
  }

  /**
   * Notifies that an un-deployment for a given artifact has finished with a failure.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being un-deployed
   * @param cause        the cause of the failure
   */
  default void onUndeploymentFailure(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName,
                                     Throwable cause) {
    onUndeploymentFailure(type.getArtifactType(), artifactName, cause);
  }

  /**
   * Notifies the artifact creation for a given artifact.
   *
   * @param type                 the artifact type
   * @param artifactName         name of the application that owns the mule context
   * @param customizationService customization service for server plugins
   */
  default void onArtifactCreated(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName,
                                 CustomizationService customizationService) {
    onArtifactCreated(type.getArtifactType(), artifactName, customizationService);
  }

  /**
   * Notifies the artifact initialisation for a given artifact.
   *
   * @param type         the artifact type
   * @param artifactName name of the application that owns the mule context
   * @param registry     mule registry for the application being deployed
   */
  default void onArtifactInitialised(org.mule.runtime.core.api.config.bootstrap.ArtifactType type, String artifactName,
                                     Registry registry) {
    onArtifactInitialised(type.getArtifactType(), artifactName, registry);
  }

  /**
   * Notifies that a deploy for a given artifact has started.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being deployed
   */
  default void onDeploymentStart(ArtifactType type, String artifactName) {

  }

  /**
   * Notifies that a deploy for a given artifact has successfully finished.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being deployed
   */
  default void onDeploymentSuccess(ArtifactType type, String artifactName) {

  }

  /**
   * Notifies that a deploy for a given artifact has finished with a failure.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being deployed
   * @param cause        the cause of the failure
   */
  default void onDeploymentFailure(ArtifactType type, String artifactName, Throwable cause) {

  }

  /**
   * Notifies that an un-deployment for a given artifact has started.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being un-deployed
   * @param type         the artifact type
   */
  default void onUndeploymentStart(ArtifactType type, String artifactName) {

  }

  /**
   * Notifies that an un-deployment for a given artifact has successfully finished.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being un-deployed
   */
  default void onUndeploymentSuccess(ArtifactType type, String artifactName) {

  }

  /**
   * Notifies that an un-deployment for a given artifact has finished with a failure.
   *
   * @param type         the artifact type
   * @param artifactName the name of the artifact being un-deployed
   * @param cause        the cause of the failure
   */
  default void onUndeploymentFailure(ArtifactType type, String artifactName, Throwable cause) {

  }

  /**
   * Notifies the artifact creation for a given artifact.
   *
   * @param type                 the artifact type
   * @param artifactName         name of the application that owns the mule context
   * @param customizationService customization service for server plugins
   */
  default void onArtifactCreated(ArtifactType type, String artifactName, CustomizationService customizationService) {

  }

  /**
   * Notifies the artifact initialisation for a given artifact.
   *
   * @param type         the artifact type
   * @param artifactName name of the application that owns the mule context
   * @param registry     mule registry for the application being deployed
   */
  default void onArtifactInitialised(ArtifactType type, String artifactName, Registry registry) {

  }

}
