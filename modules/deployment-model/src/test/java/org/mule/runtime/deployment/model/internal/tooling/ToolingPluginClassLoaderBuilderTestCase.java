/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.tooling;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.deployment.model.internal.tooling.ToolingPluginClassLoaderBuilder.getPluginArtifactClassLoader;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.PluginResolutionError;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;

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
