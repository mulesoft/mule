/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.module.deployment.api.DeploymentListener;

/**
 * Delegates {@link MuleContextListener} notifications to a {@link DeploymentListener}
 */
public class MuleContextDeploymentListener implements MuleContextListener {

  private final String appName;
  private final DeploymentListener deploymentListener;

  public MuleContextDeploymentListener(String appName, DeploymentListener deploymentListener) {
    this.appName = appName;
    this.deploymentListener = deploymentListener;
  }

  @Override
  public void onCreation(MuleContext context) {
    deploymentListener.onArtifactCreated(appName, context.getCustomizationService());
  }

  @Override
  public void onInitialization(MuleContext context, Registry registry) {
    deploymentListener.onArtifactInitialised(appName, registry);
  }

  @Override
  public void onStart(MuleContext context, Registry registry) {
    deploymentListener.onArtifactStarted(appName, registry);
  }

  @Override
  public void onStop(MuleContext context, Registry registry) {
    deploymentListener.onArtifactStopped(appName, registry);
  }
}
