/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.tck.junit4.FlakinessDetectorTestRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

/**
 * Contains tests for leak prevention on the deployment process.
 */
@RunWith(FlakinessDetectorTestRunner.class)
@Feature(LEAK_PREVENTION)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
public class ClassLoaderLeakOnDeploymentTestCaseConfig2 extends ClassLoaderLeakTestCase {

  @Parameterized.Parameters(name = "Parallel: {0}, AppName: {1}, Use Plugin: {2}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false, "empty-config-1.0.0-mule-application",
            "empty-config", false},
        {true, "empty-config-1.0.0-mule-application",
            "empty-config", false},
        {false, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-extension-plugin-config", true},
        {true, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-extension-plugin-config", true}
    });
  }

  public ClassLoaderLeakOnDeploymentTestCaseConfig2() {
    super(true, "empty-config-1.0.0-mule-application",
          "empty-config", false);
  }
}
