/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.tooling;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.classloader.TestArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;

public abstract class AbstractToolingClassLoaderTestCase extends AbstractMuleTestCase {

  protected static final String PLUGIN_NAME = "test-plugin";
  protected static final String TEST_REGION = "test-region";
  protected static final String REGION_NAME = "test-region-descriptor";

  protected RegionClassLoader regionClassLoader;
  protected ArtifactPluginDescriptor artifactPluginDescriptor;
  protected ToolingArtifactClassLoaderTestCase.TestToolingPluginClassLoader pluginArtifactClassLoader;

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

  /**
   * Helper class to determine if the disposal of the objects were done properly
   */
  public static class TestToolingPluginClassLoader extends TestArtifactClassLoader implements DisposableClassLoader {

    private final ArtifactPluginDescriptor artifactPluginDescriptor;
    protected boolean disposed = false;

    public TestToolingPluginClassLoader(ArtifactPluginDescriptor artifactPluginDescriptor) {
      super(null);
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
