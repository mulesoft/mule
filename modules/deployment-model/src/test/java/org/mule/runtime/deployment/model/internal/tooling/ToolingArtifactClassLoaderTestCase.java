/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.internal.tooling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import org.junit.Test;

public class ToolingArtifactClassLoaderTestCase extends AbstractToolingClassLoaderTestCase {

  @Test(expected = NullPointerException.class)
  public void createClassLoaderWithoutDelegate() {
    new ToolingArtifactClassLoader(regionClassLoader, null);
  }

  @Test
  public void createsClassLoaderSinglePlugin() throws Exception {
    regionClassLoader.addClassLoader(pluginArtifactClassLoader, mock(ArtifactClassLoaderFilter.class));
    ToolingArtifactClassLoader toolingArtifactClassLoader =
        new ToolingArtifactClassLoader(regionClassLoader, pluginArtifactClassLoader);
    assertThat(regionClassLoader.getArtifactPluginClassLoaders().size(), is(1));
    assertThat(pluginArtifactClassLoader.disposed, is(false));
    toolingArtifactClassLoader.dispose();
    assertThat(pluginArtifactClassLoader.disposed, is(true));
  }

  @Test
  public void createsClassLoaderMultiplePlugin() throws Exception {
    TestToolingPluginClassLoader anotherPluginClassLoader =
        new TestToolingPluginClassLoader(new ArtifactPluginDescriptor("test-another-plugin-descriptor"));
    regionClassLoader.addClassLoader(anotherPluginClassLoader, mock(ArtifactClassLoaderFilter.class));

    regionClassLoader.addClassLoader(pluginArtifactClassLoader, mock(ArtifactClassLoaderFilter.class));
    ToolingArtifactClassLoader toolingArtifactClassLoader =
        new ToolingArtifactClassLoader(regionClassLoader, pluginArtifactClassLoader);
    assertThat(regionClassLoader.getArtifactPluginClassLoaders().size(), is(2));
    assertThat(pluginArtifactClassLoader.disposed, is(false));
    assertThat(anotherPluginClassLoader.disposed, is(false));
    toolingArtifactClassLoader.dispose();
    assertThat(pluginArtifactClassLoader.disposed, is(true));
    assertThat(anotherPluginClassLoader.disposed, is(true));
  }

}
