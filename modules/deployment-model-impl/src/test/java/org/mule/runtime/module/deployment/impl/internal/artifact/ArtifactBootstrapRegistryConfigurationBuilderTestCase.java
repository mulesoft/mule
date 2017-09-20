/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;

import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.internal.util.EnumerationAdapter;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class ArtifactBootstrapRegistryConfigurationBuilderTestCase extends AbstractMuleContextTestCase {

  @Inject
  @Named("testObject1")
  private Object testObject1;

  @Inject
  @Named("testObject2")
  private Object testObject2;

  public ArtifactBootstrapRegistryConfigurationBuilderTestCase() {
    setStartContext(false);
  }

  @Override
  protected void doSetUp() throws Exception {
    muleContext.getInjector().inject(this);
  }

  @Test
  public void usesAllPluginBootstrapFiles() throws Exception {
    assertThat(testObject1, is(not(nullValue())));
    assertThat(testObject2, is(not(nullValue())));
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(0, createBootstrapServiceDiscovererContextBuilder());

    super.addBuilders(builders);
  }

  private ArtifactBootstrapServiceDiscovererConfigurationBuilder createBootstrapServiceDiscovererContextBuilder() {
    try {
      ArtifactPlugin plugin1 = mock(ArtifactPlugin.class);
      final ArtifactClassLoader pluginClassLoader1 = mock(ArtifactClassLoader.class);
      when(plugin1.getArtifactClassLoader()).thenReturn(pluginClassLoader1);
      final List<ArtifactPlugin> artifactPlugins = new ArrayList<>();
      artifactPlugins.add(plugin1);

      final List<URL> urls = new ArrayList<>();
      urls.add(this.getClass().getResource("/plugin1-bootstrap.properties"));
      urls.add(this.getClass().getResource("/plugin2-bootstrap.properties"));
      when(pluginClassLoader1.findResources(BOOTSTRAP_PROPERTIES)).thenReturn(new EnumerationAdapter<>(urls));
      return new ArtifactBootstrapServiceDiscovererConfigurationBuilder(artifactPlugins);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
