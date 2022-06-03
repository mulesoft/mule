/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver.pluginDescriptorResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver.pluginModelResolver;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.util.Collections.emptyMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class DefaultDeployableArtifactDescriptorFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void createBasicApplicationDescriptor() throws Exception {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/basic");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), contains("org.test"));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(),
               containsInAnyOrder("test-script.dwl", "app.xml"));

    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(), hasSize(3));
  }

  @Test
  public void createApplicationDescriptorWithSharedLibrary() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/shared-lib");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), everyItem(startsWith("org.apache.derby")));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(), hasItems(startsWith("org/apache/derby")));
  }

  @Test
  public void createApplicationDescriptorWithAdditionalPluginDependency() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/additional-plugin-dependency");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), hasSize(0));
    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(), hasSize(4));

    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("derby")))));

    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies(),
               hasItem(hasProperty("descriptor", hasProperty("artifactId", equalTo("mule-http-connector")))));

    List<BundleDependency> additionalDependencies =
        applicationDescriptor.getClassLoaderModel().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-http-connector")).findAny()
            .get().getAdditionalDependenciesList();

    assertThat(additionalDependencies, hasSize(1));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("derby"));
  }

  @Test
  public void createApplicationDescriptorWithExportedPackagesAndResourcesInMuleArtifactJson() throws URISyntaxException {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/exported-packages-resources-model");

    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), contains("org.exported-test"));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(), contains("exported-test-script.dwl"));
  }

  private ApplicationDescriptor createApplicationDescriptor(String appPath) throws URISyntaxException {
    MavenDeployableProjectModelBuilder modelFactory =
        new MavenDeployableProjectModelBuilder(getApplicationFolder(appPath));

    DeployableProjectModel model = modelFactory.build();

    DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory = new DefaultDeployableArtifactDescriptorFactory();
    return deployableArtifactDescriptorFactory.createApplicationDescriptor(model, emptyMap(),
                                                                           pluginModelResolver(),
                                                                           pluginDescriptorResolver());
  }

  protected File getApplicationFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
