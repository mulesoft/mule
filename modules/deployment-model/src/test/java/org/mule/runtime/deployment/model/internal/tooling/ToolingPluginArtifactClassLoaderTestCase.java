/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.tooling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.PluginResolutionError;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.classloader.TestArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class ToolingPluginArtifactClassLoaderTestCase extends AbstractMuleTestCase {

  private static final String PLUGIN_NAME = "test-plugin";
  private static final String TEST_REGION = "test-region";
  private static final String REGION_NAME = "test-region-descriptor";

  private RegionClassLoader regionClassLoader;
  private ArtifactPluginDescriptor artifactPluginDescriptor;
  private TestToolingPluginClassLoader pluginArtifactClassLoader;

  @Before
  public void createAppClassLoader() {
    final ClassLoaderLookupPolicy classLoaderLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    // Mandatory to find a resource releaser instance when doing the dispose of a RegionClassLoader
    when(classLoaderLookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);

    regionClassLoader =
        new RegionClassLoader(TEST_REGION, new ArtifactDescriptor(REGION_NAME), getClass().getClassLoader(),
                              classLoaderLookupPolicy);
    // Loading the additional classloader as the ToolingPluginClassLoaderBuilder does
    regionClassLoader.addClassLoader(mock(ArtifactClassLoader.class), mock(ArtifactClassLoaderFilter.class));
    artifactPluginDescriptor = new ArtifactPluginDescriptor(PLUGIN_NAME);
    pluginArtifactClassLoader = spy(new TestToolingPluginClassLoader(artifactPluginDescriptor));
  }

  @Test(expected = PluginResolutionError.class)
  public void createClassLoaderWithEmptyPluginList() {
    new ToolingPluginArtifactClassLoader(regionClassLoader, artifactPluginDescriptor);
  }

  @Test
  public void createsClassLoaderSinglePlugin() throws Exception {
    regionClassLoader.addClassLoader(pluginArtifactClassLoader, mock(ArtifactClassLoaderFilter.class));
    ToolingPluginArtifactClassLoader toolingPluginArtifactClassLoader =
        new ToolingPluginArtifactClassLoader(regionClassLoader, artifactPluginDescriptor);
    assertThat(regionClassLoader.getArtifactPluginClassLoaders().size(), is(1));
    assertThat(pluginArtifactClassLoader.disposed, is(false));
    toolingPluginArtifactClassLoader.dispose();
    assertThat(pluginArtifactClassLoader.disposed, is(true));
  }

  @Test
  public void createsClassLoaderMultiplePlugin() throws Exception {
    TestToolingPluginClassLoader anotherPluginClassLoader =
        new TestToolingPluginClassLoader(new ArtifactPluginDescriptor("test-another-plugin-descriptor"));
    regionClassLoader.addClassLoader(anotherPluginClassLoader, mock(ArtifactClassLoaderFilter.class));

    regionClassLoader.addClassLoader(pluginArtifactClassLoader, mock(ArtifactClassLoaderFilter.class));
    ToolingPluginArtifactClassLoader toolingPluginArtifactClassLoader =
        new ToolingPluginArtifactClassLoader(regionClassLoader, artifactPluginDescriptor);
    assertThat(regionClassLoader.getArtifactPluginClassLoaders().size(), is(2));
    assertThat(pluginArtifactClassLoader.disposed, is(false));
    assertThat(anotherPluginClassLoader.disposed, is(false));
    toolingPluginArtifactClassLoader.dispose();
    assertThat(pluginArtifactClassLoader.disposed, is(true));
    assertThat(anotherPluginClassLoader.disposed, is(true));
  }

  /**
   * Helper class to determine if the disposal of the objects were done properly
   */
  private static class TestToolingPluginClassLoader extends TestArtifactClassLoader implements DisposableClassLoader {

    private final ArtifactPluginDescriptor artifactPluginDescriptor;
    private boolean disposed = false;

    public TestToolingPluginClassLoader(ArtifactPluginDescriptor artifactPluginDescriptor) {
      this.artifactPluginDescriptor = artifactPluginDescriptor;
    }

    @Override
    public String getArtifactId() {
      return getArtifactPluginId("parentId", artifactPluginDescriptor.getName());
    }

    @Override
    public void dispose() {
      this.disposed = true;
    }
  }
}
