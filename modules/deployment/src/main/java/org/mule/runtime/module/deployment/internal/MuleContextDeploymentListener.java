/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
  public void onConfiguration(MuleContext context) {}
}
