/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.SINGLE_APP_MODE_PROPERTY;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentSuccessfulStory.DEPLOYMENT_SUCCESS;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;

import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(APP_DEPLOYMENT)
@Story(SINGLE_APP_DEPLOYMENT)
public class SingleAppDeploymentTestCase extends AbstractApplicationDeploymentTestCase {

  @Rule
  public SystemProperty singleAppModeProperty;

  @Parameterized.Parameters(name = "Parallel Deployment: {0} - Single Deployment: {1}")
  public static List<Object[]> parameters() {
    return singletonList(new Object[] {FALSE, TRUE});
  }

  public SingleAppDeploymentTestCase(boolean parallelDeployment, boolean singleDeployment) {
    super(parallelDeployment);
    if (singleDeployment) {
      this.singleAppModeProperty = new SystemProperty(SINGLE_APP_MODE_PROPERTY, "");
    }
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysSingleAppZipOnStartup() throws Exception {
    final int totalApps = 3;

    for (int i = 1; i <= totalApps; i++) {
      addExplodedAppFromBuilder(appFileBuilder(Integer.toString(i), emptyAppFileBuilder));
    }

    startDeployment();

    triggerDirectoryWatcher();

    assertApplicationAnchorFileExists(appFileBuilder("1", emptyAppFileBuilder).getId());
    assertApplicationAnchorFileDoesNotExists(appFileBuilder("2", emptyAppFileBuilder).getId());
    assertApplicationAnchorFileDoesNotExists(appFileBuilder("3", emptyAppFileBuilder).getId());
  }
}
