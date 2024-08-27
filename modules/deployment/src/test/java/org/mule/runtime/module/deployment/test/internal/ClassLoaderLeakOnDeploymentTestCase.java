/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static java.util.Set.of;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.bridgeMethodExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV1Plugin;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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

  public static final Supplier<Set<ArtifactPluginFileBuilder>> BRIDGE_METHOD_PLUGIN = () -> of(bridgeMethodExtensionPlugin);
  public static final Supplier<Set<ArtifactPluginFileBuilder>> HELLO_V1_PLUGIN = () -> of(helloExtensionV1Plugin);
  public static final Supplier<Set<ArtifactPluginFileBuilder>> NO_PLUGINS = () -> emptySet();

  @Parameterized.Parameters(name = "Parallel: {0}, AppName: {1}, Use Plugin: {2}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false, "empty-config-1.0.0-mule-application",
            "empty-config", NO_PLUGINS},
        {true, "empty-config-1.0.0-mule-application",
            "empty-config", NO_PLUGINS},
        {false, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-extension-plugin-config", HELLO_V1_PLUGIN},
        {true, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-extension-plugin-config", HELLO_V1_PLUGIN},
        {false, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-bridge-extension-plugin-config",
            BRIDGE_METHOD_PLUGIN},
        {true, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-bridge-extension-plugin-config",
            BRIDGE_METHOD_PLUGIN}
    });
  }

  public ClassLoaderLeakOnDeploymentTestCase(boolean parallellDeployment, String appName, String xmlFile,
                                             Supplier<Set<ArtifactPluginFileBuilder>> applicationPlugins) {
    super(parallellDeployment, appName, xmlFile, applicationPlugins);
  }
}
