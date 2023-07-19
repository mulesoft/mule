/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.internal.tooling;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.deployment.model.internal.tooling.ToolingPluginClassLoaderBuilder.getPluginArtifactClassLoader;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginResolutionError;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class ToolingPluginClassLoaderBuilderTestCase extends AbstractToolingClassLoaderTestCase {

  @Test(expected = PluginResolutionError.class)
  public void createClassLoaderWithEmptyPluginList() {
    getPluginArtifactClassLoader(artifactPluginDescriptor, Collections.emptyList());
  }

  @Test
  public void findArtifactClassLoaderByArtifactId() {
    TestToolingPluginClassLoader anotherPluginClassLoader =
        new TestToolingPluginClassLoader(new ArtifactPluginDescriptor("test-another-plugin-descriptor"));
    regionClassLoader.addClassLoader(anotherPluginClassLoader, mock(ArtifactClassLoaderFilter.class));
    regionClassLoader.addClassLoader(pluginArtifactClassLoader, mock(ArtifactClassLoaderFilter.class));

    ArtifactClassLoader pluginArtifactClassLoader =
        getPluginArtifactClassLoader(artifactPluginDescriptor, regionClassLoader.getArtifactPluginClassLoaders());

    assertThat(pluginArtifactClassLoader, CoreMatchers.sameInstance(pluginArtifactClassLoader));
  }

}
