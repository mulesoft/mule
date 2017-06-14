/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.MuleContextListenerFactory;

/**
 * Creates {@link MuleContextListener} instances to tied the mule context lifecycle to the deployment
 * lifecycle.
 */
public class DeploymentMuleContextListenerFactory implements MuleContextListenerFactory {

  private final DeploymentListener deploymentListener;

  /**
   * Creates a new factory to link created contexts with a given deployment listener
   *
   * @param deploymentListener deployment listener to be notified for each context lifecycle notification. Non null.
   */
  public DeploymentMuleContextListenerFactory(DeploymentListener deploymentListener) {
    checkArgument(deploymentListener != null, "deploymentListener cannot be null");

    this.deploymentListener = deploymentListener;
  }

  @Override
  public MuleContextListener create(String artifactName) {
    checkArgument(!isEmpty(artifactName), "artifactName cannot be empty");

    return new MuleContextDeploymentListener(artifactName, deploymentListener);
  }
}
