/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.environment.applicationserver;

import org.mule.runtime.environment.RuntimeEnvironment;
import org.mule.runtime.module.deployment.api.DeploymentService;

import javax.inject.Inject;

public class ApplicationServerRuntimeEnvironment implements RuntimeEnvironment {

  @Inject
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
}
