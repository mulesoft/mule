/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.environment.singleapp.api;

import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RUNTIME_ENVIRONMENT;
import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RuntimeEnvironmentStory.SINGLE_APP_ENVIRONMENT;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.environment.singleapp.impl.SingleAppEnvironment;

@Feature(RUNTIME_ENVIRONMENT)
@Story(SINGLE_APP_ENVIRONMENT)
public class SingleAppEnvironmentTestCase {

  @Test
  @Description("When environment is started, the app executor starts the app")
  public void whenExecutorStartsDeploymentServiceIsStarted() {
    SingleAppEnvironment applicationServerRuntimeEnvironment = new SingleAppEnvironment();
    SingleAppStarter singleAppExecutor = mock(SingleAppStarter.class);
    applicationServerRuntimeEnvironment.setSingleAppStarter(singleAppExecutor);
    applicationServerRuntimeEnvironment.start();
    verify(singleAppExecutor).startApp();
  }
}
