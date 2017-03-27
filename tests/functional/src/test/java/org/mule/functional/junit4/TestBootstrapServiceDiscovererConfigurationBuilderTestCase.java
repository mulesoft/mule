/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.EnumerationAdapter;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class TestBootstrapServiceDiscovererConfigurationBuilderTestCase extends AbstractMuleContextTestCase {

  public TestBootstrapServiceDiscovererConfigurationBuilderTestCase() {
    setStartContext(false);
  }

  @Test
  public void usesAllPluginBootstrapFiles() throws Exception {
    assertThat(muleContext.getRegistry().lookupObject("testObject1"), is(not(nullValue())));
    assertThat(muleContext.getRegistry().lookupObject("testObject2"), is(not(nullValue())));
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(0, createBootstrapServiceDiscovererContextBuilder());

    super.addBuilders(builders);
  }

  private TestBootstrapServiceDiscovererConfigurationBuilder createBootstrapServiceDiscovererContextBuilder() {
    try {
      final ClassLoaderFilter filter = mock(ClassLoaderFilter.class);
      when(filter.exportsClass(anyString())).thenReturn(true);

      final ArtifactClassLoader pluginClassLoader1 = mock(ArtifactClassLoader.class);
      when(pluginClassLoader1.getClassLoader()).thenReturn(this.getClass().getClassLoader());
      final List<ArtifactClassLoader> artifactClassLoaders = new ArrayList<>();
      artifactClassLoaders.add(new FilteringArtifactClassLoader(pluginClassLoader1, filter, Collections.emptyList()));

      final List<URL> urls = new ArrayList<>();
      urls.add(this.getClass().getResource("/plugin1-bootstrap.properties"));
      urls.add(this.getClass().getResource("/plugin2-bootstrap.properties"));
      when(pluginClassLoader1.findResources(BOOTSTRAP_PROPERTIES)).thenReturn(new EnumerationAdapter<>(urls));

      final ArtifactClassLoader appClassLoader = mock(ArtifactClassLoader.class);
      when(appClassLoader.getClassLoader()).thenReturn(this.getClass().getClassLoader());
      when(appClassLoader.findResources(BOOTSTRAP_PROPERTIES)).thenReturn(new EnumerationAdapter<>(emptyList()));
      final ClassLoader executionClassLoader = new FilteringArtifactClassLoader(appClassLoader, filter, Collections.emptyList());
      return new TestBootstrapServiceDiscovererConfigurationBuilder(getClass().getClassLoader(), executionClassLoader,
                                                                    artifactClassLoaders);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
