/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import static java.util.Arrays.asList;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import java.util.List;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Contains tests for leak prevention on the deployment process.
 */
@RunWith(Parameterized.class)
@Feature(LEAK_PREVENTION)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
@Issues({@Issue("W-13160893"), @Issue("MULE-17311")})
public class ClassLoaderLeakOnDeploymentTestCase extends ClassLoaderLeakTestCase {

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

  public ClassLoaderLeakOnDeploymentTestCase(boolean parallellDeployment, String appName, String xmlFile,
                                             boolean useEchoPluginInApp) {
    super(parallellDeployment, appName, xmlFile, useEchoPluginInApp);
  }
}
