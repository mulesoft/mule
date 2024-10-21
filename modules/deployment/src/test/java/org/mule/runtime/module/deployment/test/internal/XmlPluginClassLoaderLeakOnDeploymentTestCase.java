/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.byeXmlExtensionPlugin;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.runners.Parameterized.Parameters;

@Feature(LEAK_PREVENTION)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
@Issue("W-16814280")
public class XmlPluginClassLoaderLeakOnDeploymentTestCase extends ClassLoaderLeakOnDeploymentTestCase {

  @ClassRule
  public static SystemProperty xmlLeak = new SystemProperty("mule.test.transformer.pool.leak", "true");

  public static final Supplier<Set<ArtifactPluginFileBuilder>> XML_SDK_PLUGIN =
      () -> new HashSet<>(singletonList(byeXmlExtensionPlugin));

  @Parameters(name = "Parallel: {0}, AppName: {1}, Use Plugin: {2}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-extension-xml-plugin-module-bye",
            XML_SDK_PLUGIN},
        {true, "appWithExtensionPlugin-1.0.0-mule-application",
            "app-with-extension-xml-plugin-module-bye",
            XML_SDK_PLUGIN}
    });
  }

  public XmlPluginClassLoaderLeakOnDeploymentTestCase(boolean parallelDeployment, String appName, String xmlFile,
                                                      Supplier<Set<ArtifactPluginFileBuilder>> applicationPlugins) {
    super(parallelDeployment, appName, xmlFile, applicationPlugins);
  }

}
