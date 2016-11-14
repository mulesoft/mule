/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.plugin;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BundlePluginDependenciesResolverTestCase extends AbstractMuleTestCase {

  private static final String FOO_PLUGIN = "foo";
  private static final String BAZ_PLUGIN = "baz";
  private static final String BAR_PLUGIN = "bar";
  public static final String DEPENDENCY_PROVIDER_ERROR_MESSAGE = "The '%s' cannot be found.";

  private static final BundleDescriptor FOO_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(FOO_PLUGIN, "1.0");
  private static final BundleDescriptor BAZ_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(BAZ_PLUGIN, "1.0");
  private static final BundleDescriptor BAR_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(BAR_PLUGIN, "1.0");

  private static final BundleDependency FOO_PLUGIN_DESCRIPTOR = createBundleDependency(FOO_BUNDLE_DESCRIPTOR);
  private static final BundleDependency BAZ_PLUGIN_DESCRIPTOR = createBundleDependency(BAZ_BUNDLE_DESCRIPTOR);
  private static final BundleDependency BAR_PLUGIN_DESCRIPTOR = createBundleDependency(BAR_BUNDLE_DESCRIPTOR);

  private static BundleDependency createBundleDependency(BundleDescriptor bundleDescriptor) {
    return new BundleDependency.Builder().setDescriptor(bundleDescriptor).setType("zip").setClassifier(MULE_PLUGIN_CLASSIFIER)
        .build();
  }

  private static BundleDescriptor createTestBundleDescriptor(String artifactId, String version) {
    return new BundleDescriptor.Builder().setGroupId("test").setArtifactId(artifactId).setVersion(version).build();
  }

  private final ArtifactPluginDescriptor fooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
  private final ArtifactPluginDescriptor barPlugin = new ArtifactPluginDescriptor(BAR_PLUGIN);
  private final ArtifactPluginDescriptor bazPlugin = new ArtifactPluginDescriptor(BAZ_PLUGIN);
  private final PluginDependenciesResolver dependenciesResolver =
      new BundlePluginDependenciesResolver(mock(ArtifactDescriptorFactory.class), artifactName -> {
        throw new PluginResolutionError(format(DEPENDENCY_PROVIDER_ERROR_MESSAGE, artifactName));
      });

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    fooPlugin.setBundleDescriptor(FOO_BUNDLE_DESCRIPTOR);
    bazPlugin.setBundleDescriptor(BAZ_BUNDLE_DESCRIPTOR);
    barPlugin.setBundleDescriptor(BAR_BUNDLE_DESCRIPTOR);
  }

  @Test
  public void resolvesIndependentPlugins() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, barPlugin, fooPlugin);
  }

  @Test
  public void resolvesPluginOrderedDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginDisorderedDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(barPlugin, fooPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginDependencyWithCompatibleMinorVersion() throws Exception {

    ArtifactPluginDescriptor updatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    updatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "1.1"));

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(updatedFooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(FOO_BUNDLE_DESCRIPTOR))).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, updatedFooPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginDependencyWithSnapshotMinorVersion() throws Exception {

    ArtifactPluginDescriptor updatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    updatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "1.1-SNAPSHOT"));

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(updatedFooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(FOO_BUNDLE_DESCRIPTOR))).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, updatedFooPlugin, barPlugin);
  }

  @Test
  public void resolvesSnapshotPluginDependencyWithCompatibleMinorVersion() throws Exception {

    ArtifactPluginDescriptor updatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    updatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "1.1"));

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(updatedFooPlugin, barPlugin);
    BundleDescriptor fooBundleDescriptor = createTestBundleDescriptor(FOO_PLUGIN, "1.0-SNAPSHOT");
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(fooBundleDescriptor))).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, updatedFooPlugin, barPlugin);
  }

  @Test(expected = PluginResolutionError.class)
  public void doesNotResolvesPluginDependencyWithIncompatibleMajorVersion() throws Exception {

    ArtifactPluginDescriptor majorUpdatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    majorUpdatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "2.0"));
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(majorUpdatedFooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(FOO_BUNDLE_DESCRIPTOR))).build());

    dependenciesResolver.resolve(pluginDescriptors);
  }


  @Test(expected = PluginResolutionError.class)
  public void doesNotResolvesPluginDependencyWithIncompatibleMinorVersion() throws Exception {
    ArtifactPluginDescriptor majorUpdatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    majorUpdatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "1.1"));

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);

    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(majorUpdatedFooPlugin.getBundleDescriptor()))).build());

    dependenciesResolver.resolve(pluginDescriptors);
  }

  @Test
  public void detectsUnresolvablePluginDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin);
    fooPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(BAR_PLUGIN_DESCRIPTOR)).build());

    expectedException.expect(PluginResolutionError.class);
    expectedException.expectMessage(format(DEPENDENCY_PROVIDER_ERROR_MESSAGE, BAR_BUNDLE_DESCRIPTOR));
    dependenciesResolver.resolve(pluginDescriptors);
  }

  @Test
  public void resolvesTransitiveDependencies() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin, bazPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(BAZ_PLUGIN_DESCRIPTOR)).build());
    bazPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, bazPlugin, barPlugin);
  }

  @Test
  public void resolvesMultipleDependencies() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin, bazPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(BAZ_PLUGIN_DESCRIPTOR)).build());
    bazPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, bazPlugin, barPlugin);
  }

  @Test
  public void sanitizesDependantPluginExportedPackages() throws Exception {
    fooPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().exportingPackages(getFooExportedPackages()).build());

    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR))
        .exportingPackages(getBarExportedPackages()).build());

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, barPlugin);
    assertPluginExportedPackages(fooPlugin, "org.foo", "org.foo.mule");
    assertPluginExportedPackages(barPlugin, "org.bar", "org.baz", "org.bar.mule");
  }

  @Test
  public void sanitizesTransitiveDependantPluginExportedPackages() throws Exception {
    fooPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().exportingPackages(getFooExportedPackages()).build());

    bazPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().exportingPackages(getBazExportedPackages())
        .dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    barPlugin.setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(getBarExportedPackages())
        .dependingOn(singleton(BAZ_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin, bazPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(pluginDescriptors);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, bazPlugin, barPlugin);
    assertPluginExportedPackages(fooPlugin, "org.foo", "org.foo.mule");
    assertPluginExportedPackages(bazPlugin, "org.baz");
    assertPluginExportedPackages(barPlugin, "org.bar", "org.bar.mule");
  }

  private Set<String> getBarExportedPackages() {
    final Set<String> barExportedClassPackages = new HashSet<>();
    barExportedClassPackages.add("org.bar");
    barExportedClassPackages.add("org.baz");
    barExportedClassPackages.add("org.foo");
    barExportedClassPackages.add("org.foo.mule");
    barExportedClassPackages.add("org.bar.mule");
    return barExportedClassPackages;
  }

  private Set<String> getFooExportedPackages() {
    final Set<String> fooExportedClassPackages = new HashSet<>();
    fooExportedClassPackages.add("org.foo");
    fooExportedClassPackages.add("org.foo.mule");
    return fooExportedClassPackages;
  }

  private Set<String> getBazExportedPackages() {
    final Set<String> bazExportedClassPackages = new HashSet<>();
    bazExportedClassPackages.add("org.baz");
    return bazExportedClassPackages;
  }

  private void assertResolvedPlugins(List<ArtifactPluginDescriptor> resolvedPluginDescriptors,
                                     ArtifactPluginDescriptor... expectedPluginDescriptors) {
    assertThat(resolvedPluginDescriptors.size(), equalTo(expectedPluginDescriptors.length));

    for (int i = 0; i < resolvedPluginDescriptors.size(); i++) {
      assertThat(resolvedPluginDescriptors.get(i), equalTo(expectedPluginDescriptors[i]));
    }
  }

  private void assertPluginExportedPackages(ArtifactPluginDescriptor pluginDescriptor, String... exportedPackages) {
    assertThat(pluginDescriptor.getClassLoaderModel().getExportedPackages().size(), equalTo(exportedPackages.length));
    assertThat(pluginDescriptor.getClassLoaderModel().getExportedPackages(), containsInAnyOrder(exportedPackages));
  }

  private List<ArtifactPluginDescriptor> createPluginDescriptors(ArtifactPluginDescriptor... descriptors) {
    final List<ArtifactPluginDescriptor> result = new ArrayList<>();
    for (ArtifactPluginDescriptor descriptor : descriptors) {
      result.add(descriptor);
    }

    return result;
  }
}
