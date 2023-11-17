/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.api.applicationserver;

import org.mule.runtime.environment.api.RuntimeEnvironment;
import org.mule.runtime.module.deployment.api.DeploymentService;

import javax.inject.Inject;

/**
 * A {@link RuntimeEnvironment} that represents a container that works as an application server, where different applications and
 * domains can be deployed and the lifecycle of those applications is independent of the lifecyce of the runtime. Starting this
 * runtime environment is starting the deployment service.
 *
 * @since 4.7.0
 */
public class ApplicationServerRuntimeEnvironment implements RuntimeEnvironment {

  DeploymentService deploymentService;

  @Override
  public String getName() {
    return "Application Server Environment";
  }

  @Override
  public String getDescription() {
    return "An Environment which works as Application Server";
  }

  @Override
  public void start() {
    deploymentService.start();
  }


  @Inject
  public void setDeploymentService(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }
}
