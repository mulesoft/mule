/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.tooling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;

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
