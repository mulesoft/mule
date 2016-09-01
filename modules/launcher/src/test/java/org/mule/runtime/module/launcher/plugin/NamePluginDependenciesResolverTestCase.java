/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.plugin;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.runtime.module.launcher.plugin.NamePluginDependenciesResolver.createResolutionErrorMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NamePluginDependenciesResolverTestCase extends AbstractMuleTestCase {


  private final ArtifactPluginDescriptor fooPlugin = createArtifactPluginDescriptor("foo");
  private final ArtifactPluginDescriptor barPlugin = createArtifactPluginDescriptor("bar");
  private final PluginDependenciesResolver dependenciesResolver = new NamePluginDependenciesResolver();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ArtifactPluginDescriptor createArtifactPluginDescriptor(String name) {
    final ArtifactPluginDescriptor result = new ArtifactPluginDescriptor();
    result.setName(name);

    return result;
  }

  @Test
  public void resolvesIndependentPlugins() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = new ArrayList<>();
    pluginDescriptors.add(fooPlugin);
    pluginDescriptors.add(barPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, barPlugin, fooPlugin);
  }

  @Test
  public void resolvesPluginOrderedDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = new ArrayList<>();
    barPlugin.setPluginDependencies(singleton("foo"));
    pluginDescriptors.add(fooPlugin);
    pluginDescriptors.add(barPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginDisorderedDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = new ArrayList<>();
    barPlugin.setPluginDependencies(singleton("foo"));
    pluginDescriptors.add(fooPlugin);
    pluginDescriptors.add(barPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, barPlugin);
  }

  @Test
  public void detectsUnresolvablePluginDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = new ArrayList<>();
    fooPlugin.setPluginDependencies(singleton("bar"));
    pluginDescriptors.add(fooPlugin);

    expectedException.expect(PluginResolutionError.class);
    expectedException.expectMessage(createResolutionErrorMessage(singletonList(fooPlugin)));
    dependenciesResolver.resolve(pluginDescriptors);
  }

  private void assertResolvedPlugins(List<ArtifactPluginDescriptor> resolvedPluginDescriptors,
                                     ArtifactPluginDescriptor... expectedPluginDescriptors) {
    assertThat(resolvedPluginDescriptors.size(), equalTo(expectedPluginDescriptors.length));

    for (int i = 0; i < resolvedPluginDescriptors.size(); i++) {
      assertThat(resolvedPluginDescriptors.get(i), equalTo(expectedPluginDescriptors[i]));
    }
  }
}
