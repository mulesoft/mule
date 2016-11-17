/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.deployment.api.DeploymentListener;

/**
 * Defines a {@link DeploymentListener} that does nothing
 */
public class NullDeploymentListener implements DeploymentListener {

  @Override
  public void onDeploymentStart(String artifactName) {}

  @Override
  public void onDeploymentSuccess(String appName) {}

  @Override
  public void onDeploymentFailure(String artifactName, Throwable cause) {}

  @Override
  public void onUndeploymentStart(String artifactName) {}

  @Override
  public void onUndeploymentSuccess(String artifactName) {}

  @Override
  public void onUndeploymentFailure(String artifactName, Throwable cause) {}

  @Override
  public void onMuleContextCreated(String artifactName, MuleContext context) {}

  @Override
  public void onMuleContextInitialised(String artifactName, MuleContext context) {}

  @Override
  public void onMuleContextConfigured(String artifactName, MuleContext context) {}
}
