/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver.pluginDescriptorResolver;
import static org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver.mavenDeployablePluginModelResolver;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.util.Collections.emptyMap;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenApplicationProjectModelFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class DefaultArtifactDescriptorFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void createApplicationDescriptor() throws Exception {
    MavenApplicationProjectModelFactory modelFactory =
        new MavenApplicationProjectModelFactory(getApplicationFolder("apps/shared-lib-additional-plugin-dependencies"));

    DeployableProjectModel<MuleApplicationModel> model = modelFactory.createDeployableProjectModel();

    ArtifactDescriptorFactory artifactDescriptorFactory = new DefaultArtifactDescriptorFactory();
    ApplicationDescriptor applicationDescriptor =
        artifactDescriptorFactory.createApplicationDescriptor(model, emptyMap(),
                                                              mavenDeployablePluginModelResolver(),
                                                              pluginDescriptorResolver());

    assertThat(applicationDescriptor.getClassLoaderModel().getDependencies().stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("derby")).findAny(),
               is(not(Optional.empty())));
    Optional<BundleDependency> pluginWithAdditionalDependencies =
        applicationDescriptor.getClassLoaderModel().getDependencies().stream()
            .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals("mule-http-connector")).findAny();
    assertThat(pluginWithAdditionalDependencies,
               is(not(Optional.empty())));
    List<BundleDependency> additionalDependencies = pluginWithAdditionalDependencies.get().getAdditionalDependenciesList();
    assertThat(additionalDependencies.size(), is(1));
    assertThat(additionalDependencies.get(0).getDescriptor().getArtifactId(), is("derby"));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedPackages(), hasItems("org.test"));
    assertThat(applicationDescriptor.getClassLoaderModel().getExportedResources(), hasItems("test-script.dwl"));
  }

  protected File getApplicationFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
