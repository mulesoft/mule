/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS_WITH_CUSTOM_LOG_CONFIG;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Story(ARTIFACT_DESCRIPTORS_WITH_CUSTOM_LOG_CONFIG)
@RunWith(Parameterized.class)
public class DeployableDescriptorsWithCustomLogConfig extends AbstractDeployableArtifactDescriptorFactoryTestCase {

  private final String deployableProjectFolder;
  private final Boolean isApplication;

  @Rule
  public final SystemProperty muleHomeSystemProperty;

  @Parameterized.Parameters(name = "deployableProjectFolder: {0}, isApplication: {1}")
  public static Collection<Pair<String, String>> deployableArtifactDescriptors() {
    return asList(
                  new Pair<>("apps/with-custom-log-config", "some-path"),
                  new Pair<>("domains/with-custom-log-config", "some-path"),
                  new Pair<>("apps/with-custom-log-config", null),
                  new Pair<>("domains/with-custom-log-config", null));
  }

  public DeployableDescriptorsWithCustomLogConfig(Pair<String, String> deployableProjectInfo) {
    deployableProjectFolder = deployableProjectInfo.getFirst();
    isApplication = deployableProjectFolder.startsWith("apps");

    muleHomeSystemProperty =
        new SystemProperty(MULE_HOME_DIRECTORY_PROPERTY, deployableProjectInfo.getSecond());
  }

  @Test
  public void createDeployableDescriptorWithCustomLogConfig() throws URISyntaxException {
    DeployableArtifactDescriptor deployableArtifactDescriptor = getDeployableArtifactDescriptor();

    String base = muleHomeSystemProperty.getValue() != null ? muleHomeSystemProperty.getValue() : ".";
    Path customLogConfig = Paths.get(base, "custom-log4j2.xml");
    assertThat(deployableArtifactDescriptor.getLogConfigFile().toPath(), is(customLogConfig));
  }

  private DeployableArtifactDescriptor getDeployableArtifactDescriptor() throws URISyntaxException {
    if (isApplication) {
      return createApplicationDescriptor(deployableProjectFolder);
    } else {
      return createDomainDescriptor(deployableProjectFolder);
    }
  }

}
