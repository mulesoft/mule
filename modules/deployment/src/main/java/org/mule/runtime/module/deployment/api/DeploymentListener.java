/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.core.api.MuleContext;

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
   * Notifies the creation of the {@link MuleContext} for a given app.
   *
   * @param artifactName name of the application that owns the mule context
   * @param context mule context for the application being deployed
   */
  default void onMuleContextCreated(String artifactName, MuleContext context, CustomizationService customizationService) {

  }

  /**
   * Notifies the initialization of the {@link MuleContext} for a given app.
   *
   * @param artifactName name of the application that owns the mule context
   * @param context mule context for the application being deployed
   */
  default void onMuleContextInitialised(String artifactName, MuleContext context) {

  }

  /**
   * Notifies the configuration of the {@link MuleContext} for a given app.
   *
   * @param artifactName name of the application that owns the mule context
   * @param context mule context for the application being deployed
   */
  default void onMuleContextConfigured(String artifactName, MuleContext context) {

  }
}
