/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.PATCHED_ARTIFACT_DESCRIPTORS;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Stories({@Story(ARTIFACT_DESCRIPTORS), @Story(PATCHED_ARTIFACT_DESCRIPTORS)})
public class DeployableDescriptorsWithPatchedPluginsTestCase extends AbstractDeployableArtifactDescriptorFactoryTestCase {

  private static final String deployableProjectModelFolder = "apps/with-patched-artifacts";

  @Rule
  public final SystemProperty muleHomeSystemProperty =
      new SystemProperty(MULE_HOME_DIRECTORY_PROPERTY, getDeployableFolder(deployableProjectModelFolder).getCanonicalPath());

  public DeployableDescriptorsWithPatchedPluginsTestCase() throws URISyntaxException, IOException {}

  @Test
  public void createApplicationDescriptorWithPatchedPlugins() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor(deployableProjectModelFolder);

    ArtifactPluginDescriptor httpPlugin = applicationDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("HTTP"))
        .findFirst()
        .get();

    assertThat(stream(httpPlugin.getClassLoaderModel().getUrls()).map(URL::toString).collect(toList()),
               hasItem(endsWith("http-patch.jar")));

    ArtifactPluginDescriptor dbPlugin = applicationDescriptor.getPlugins()
        .stream()
        .filter(p -> p.getName().equals("Database"))
        .findFirst()
        .get();

    assertThat(stream(dbPlugin.getClassLoaderModel().getUrls()).map(URL::toString).collect(toList()),
               not(hasItem(endsWith("db-patch.jar"))));
  }

}
