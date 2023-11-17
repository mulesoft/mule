/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.applicationserver.impl;

import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RUNTIME_ENVIRONMENT;
import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RuntimeEnvironmentStory.APPLICATION_SERVER_ENVIRONMENT;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.applicationserver.impl.impl.ApplicationServerRuntimeEnvironment;
import org.mule.runtime.module.deployment.api.DeploymentService;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(RUNTIME_ENVIRONMENT)
@Story(APPLICATION_SERVER_ENVIRONMENT)
public class ApplicationServerRuntimeEnvironmentTestCase {

  @Test
  @Description("When environment is started, the deployment service is started")
  public void whenExecutorStartsDeploymentServiceIsStarted() {
    ApplicationServerRuntimeEnvironment applicationServerRuntimeEnvironment = new ApplicationServerRuntimeEnvironment();
    DeploymentService deploymentService = mock(DeploymentService.class);
    applicationServerRuntimeEnvironment.setDeploymentService(deploymentService);
    applicationServerRuntimeEnvironment.start();
    verify(deploymentService).start();
  }
}
