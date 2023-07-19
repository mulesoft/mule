/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(CLASSLOADER_CONFIGURATION)
public class ClassLoaderConfigurationTestCase extends AbstractMuleTestCase {

  @Test
  public void sanitizesExportedResources() {
    Set<String> exportedResources = new HashSet<>();
    exportedResources.add("META-INF\\schemas\\schema.xml");
    exportedResources.add("META-INF\\README.txt");
    ClassLoaderConfiguration classLoaderConfiguration =
        new ClassLoaderConfiguration.ClassLoaderConfigurationBuilder().exportingResources(exportedResources).build();

    assertThat(classLoaderConfiguration.getExportedResources(),
               containsInAnyOrder("META-INF/schemas/schema.xml", "META-INF/README.txt"));
  }
}
