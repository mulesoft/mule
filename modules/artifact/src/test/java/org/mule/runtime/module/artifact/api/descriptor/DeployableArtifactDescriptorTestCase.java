/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class DeployableArtifactDescriptorTestCase extends AbstractMuleTestCase {

  @Test
  public void sanitizesConfigResources() {
    DeployableArtifactDescriptor descriptor = new DeployableArtifactDescriptor("test");
    Set<String> configResources = new HashSet<>();
    configResources.add("config\\db\\connection.xml");
    configResources.add("config\\db\\flows.xml");

    descriptor.setConfigResources(configResources);

    assertThat(descriptor.getConfigResources(), containsInAnyOrder("config/db/connection.xml", "config/db/flows.xml"));
  }
}
