/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.PATCHED_ARTIFACT_DESCRIPTORS;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Story(PATCHED_ARTIFACT_DESCRIPTORS)
@RunWith(Parameterized.class)
public class DeployableDescriptorsWithPatchedPluginsTestCase extends AbstractDeployableArtifactDescriptorFactoryTestCase {

  private static final String PATCHES_LOCATION = "/lib/patches/mule-artifact-patches";

  private final String deployableProjectFolder;
  private final Boolean isApplication;

  @Rule
  public final SystemProperty muleHomeSystemProperty;

  @Parameters(name = "Deployable project folder: {0}")
  public static Collection<String> deployableArtifactDescriptors() {
    return asList("apps/with-patched-artifacts", "domains/with-patched-artifacts");
  }

  public DeployableDescriptorsWithPatchedPluginsTestCase(String deployableProjectFolder)
      throws URISyntaxException, IOException {
    this.deployableProjectFolder = deployableProjectFolder;
    isApplication = deployableProjectFolder.startsWith("apps");

    muleHomeSystemProperty =
        new SystemProperty(MULE_HOME_DIRECTORY_PROPERTY, getDeployableFolder(deployableProjectFolder).getCanonicalPath());
  }

  @Test
  public void createDeployableDescriptorWithPatchedPlugins() throws URISyntaxException {
    DeployableArtifactDescriptor deployableArtifactDescriptor = getDeployableArtifactDescriptor();

    ArtifactPluginDescriptor httpPlugin = deployableArtifactDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("HTTP"))
        .findFirst()
        .get();

    assertThat(stream(httpPlugin.getClassLoaderModel().getUrls()).collect(toList()),
               hasItem(getClass().getClassLoader().getResource(deployableProjectFolder + PATCHES_LOCATION + "/http-patch.jar")));

    ArtifactPluginDescriptor dbPlugin = deployableArtifactDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("Database"))
        .findFirst()
        .get();

    assertThat(stream(dbPlugin.getClassLoaderModel().getUrls()).map(URL::toString).collect(toList()),
               not(hasItem(getClass().getClassLoader()
                   .getResource(deployableProjectFolder + PATCHES_LOCATION + "/db-patch.jar"))));
  }

  private DeployableArtifactDescriptor getDeployableArtifactDescriptor() throws URISyntaxException {
    if (isApplication) {
      return createApplicationDescriptor(deployableProjectFolder);
    } else {
      return createDomainDescriptor(deployableProjectFolder);
    }
  }

}
