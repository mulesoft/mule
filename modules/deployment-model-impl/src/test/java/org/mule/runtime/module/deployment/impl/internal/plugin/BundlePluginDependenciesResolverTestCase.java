/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.plugin.BundlePluginDependenciesResolver.MULE_HTTP_CONNECTOR_ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.plugin.BundlePluginDependenciesResolver.MULE_HTTP_CONNECTOR_GROUP_ID;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.DuplicateExportedPackageException;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.plugin.PluginResolutionError;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class BundlePluginDependenciesResolverTestCase extends AbstractMuleTestCase {

  private static final String FOO_PLUGIN = "foo";
  private static final String BAZ_PLUGIN = "baz";
  private static final String BAR_PLUGIN = "bar";
  private static final String ECHO_PLUGIN = "echo";
  public static final String DEPENDENCY_PROVIDER_ERROR_MESSAGE = "Bundle URL should have been resolved for %s.";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static BundleDescriptor FOO_BUNDLE_DESCRIPTOR;
  private static BundleDescriptor BAZ_BUNDLE_DESCRIPTOR;
  private static BundleDescriptor LATEST_BAZ_BUNDLE_DESCRIPTOR;
  private static BundleDescriptor BAR_BUNDLE_DESCRIPTOR;
  private static BundleDescriptor ECHO_BUNDLE_DESCRIPTOR;
  private static BundleDescriptor LATEST_ECHO_BUNDLE_DESCRIPTOR;

  private static BundleDependency FOO_PLUGIN_DESCRIPTOR;
  private static BundleDependency BAZ_PLUGIN_DESCRIPTOR;
  private static BundleDependency LATEST_BAZ_PLUGIN_DESCRIPTOR;
  private static BundleDependency BAR_PLUGIN_DESCRIPTOR;
  private static BundleDependency ECHO_PLUGIN_DESCRIPTOR;
  private static BundleDependency LATEST_ECHO_PLUGIN_DESCRIPTOR;

  @BeforeClass
  public static void before() {
    FOO_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(FOO_PLUGIN, "1.0");
    BAZ_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(BAZ_PLUGIN, "1.0");
    LATEST_BAZ_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(BAZ_PLUGIN, "1.1");
    BAR_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(BAR_PLUGIN, "1.0");
    ECHO_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(ECHO_PLUGIN, "1.0");
    LATEST_ECHO_BUNDLE_DESCRIPTOR = createTestBundleDescriptor(ECHO_PLUGIN, "1.1");

    FOO_PLUGIN_DESCRIPTOR = createBundleDependency(FOO_BUNDLE_DESCRIPTOR);
    BAZ_PLUGIN_DESCRIPTOR = createBundleDependency(BAZ_BUNDLE_DESCRIPTOR, true);
    LATEST_BAZ_PLUGIN_DESCRIPTOR = createBundleDependency(LATEST_BAZ_BUNDLE_DESCRIPTOR, true);
    BAR_PLUGIN_DESCRIPTOR = createBundleDependency(BAR_BUNDLE_DESCRIPTOR);
    ECHO_PLUGIN_DESCRIPTOR = createBundleDependency(ECHO_BUNDLE_DESCRIPTOR);
    LATEST_ECHO_PLUGIN_DESCRIPTOR = createBundleDependency(LATEST_ECHO_BUNDLE_DESCRIPTOR, true);
  }

  private static BundleDependency createBundleDependency(BundleDescriptor bundleDescriptor) {
    return createBundleDependency(bundleDescriptor, false);
  }

  private static BundleDependency createBundleDependency(BundleDescriptor bundleDescriptor, boolean createBundleUri) {
    try {
      final BundleDependency.Builder builder = new BundleDependency.Builder();
      builder.setDescriptor(bundleDescriptor);
      builder.setScope(BundleScope.COMPILE);
      if (createBundleUri) {
        builder.setBundleUri(temporaryFolder.newFile(bundleDescriptor.getArtifactFileName()).toURI());
      }
      return builder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static BundleDescriptor createTestBundleDescriptor(String artifactId, String version) {
    return new BundleDescriptor.Builder().setGroupId("test").setArtifactId(artifactId).setVersion(version)
        .setType(EXTENSION_BUNDLE_TYPE).setClassifier(MULE_PLUGIN_CLASSIFIER).build();
  }

  private final ArtifactPluginDescriptor fooPlugin = newArtifactPluginDescriptor(FOO_PLUGIN);
  private final ArtifactPluginDescriptor barPlugin = newArtifactPluginDescriptor(BAR_PLUGIN);
  private final ArtifactPluginDescriptor bazPlugin = newArtifactPluginDescriptor(BAZ_PLUGIN);
  private final ArtifactPluginDescriptor latestBazPlugin = newArtifactPluginDescriptor(BAZ_PLUGIN);
  private final ArtifactPluginDescriptor echoPlugin = newArtifactPluginDescriptor(ECHO_PLUGIN);
  private final ArtifactPluginDescriptor latestEchoPlugin = newArtifactPluginDescriptor(ECHO_PLUGIN);

  private PluginDependenciesResolver dependenciesResolver;

  private ArtifactDescriptorFactory artifactDescriptorFactory;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() throws Exception {
    fooPlugin.setBundleDescriptor(FOO_BUNDLE_DESCRIPTOR);
    bazPlugin.setBundleDescriptor(BAZ_BUNDLE_DESCRIPTOR);
    latestBazPlugin.setBundleDescriptor(LATEST_BAZ_BUNDLE_DESCRIPTOR);
    barPlugin.setBundleDescriptor(BAR_BUNDLE_DESCRIPTOR);
    echoPlugin.setBundleDescriptor(ECHO_BUNDLE_DESCRIPTOR);
    latestEchoPlugin.setBundleDescriptor(LATEST_ECHO_BUNDLE_DESCRIPTOR);

    artifactDescriptorFactory = mock(ArtifactDescriptorFactory.class);
    dependenciesResolver = new BundlePluginDependenciesResolver(artifactDescriptorFactory);
  }

  private static ArtifactPluginDescriptor newArtifactPluginDescriptor(String name) {
    return new ArtifactPluginDescriptor(name);
  }

  @Test
  public void resolvesIndependentPlugins() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, barPlugin, fooPlugin);
  }

  @Test
  public void resolvesPluginOrderedDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginDisorderedDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(barPlugin, fooPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginDependencyWithCompatibleMinorVersion() throws Exception {

    ArtifactPluginDescriptor updatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    updatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "1.1"));

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(updatedFooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(FOO_BUNDLE_DESCRIPTOR))).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, updatedFooPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginDependencyWithSnapshotMinorVersion() throws Exception {

    ArtifactPluginDescriptor updatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    updatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "1.1-SNAPSHOT"));

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(updatedFooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(FOO_BUNDLE_DESCRIPTOR))).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

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

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, updatedFooPlugin, barPlugin);
  }

  @Test(expected = PluginResolutionError.class)
  public void doesNotResolvesPluginDependencyWithIncompatibleMajorVersion() throws Exception {

    ArtifactPluginDescriptor majorUpdatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    majorUpdatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "2.0"));
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(majorUpdatedFooPlugin, barPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(FOO_BUNDLE_DESCRIPTOR))).build());

    dependenciesResolver.resolve(emptySet(), pluginDescriptors, true);
  }


  @Test(expected = PluginResolutionError.class)
  public void doesNotResolvesPluginDependencyWithIncompatibleMinorVersion() throws Exception {
    ArtifactPluginDescriptor majorUpdatedFooPlugin = new ArtifactPluginDescriptor(FOO_PLUGIN);
    majorUpdatedFooPlugin.setBundleDescriptor(createTestBundleDescriptor(FOO_PLUGIN, "1.1"));

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);

    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder()
        .dependingOn(singleton(createBundleDependency(majorUpdatedFooPlugin.getBundleDescriptor()))).build());

    dependenciesResolver.resolve(emptySet(), pluginDescriptors, true);
  }

  @Test
  public void detectsUnresolvablePluginDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin);
    fooPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(BAR_PLUGIN_DESCRIPTOR)).build());

    expectedException.expect(PluginResolutionError.class);
    expectedException.expectMessage(format(DEPENDENCY_PROVIDER_ERROR_MESSAGE, BAR_BUNDLE_DESCRIPTOR));
    dependenciesResolver.resolve(emptySet(), pluginDescriptors, true);
  }

  @Test
  public void resolvesTransitiveDependencies() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin, bazPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(BAZ_PLUGIN_DESCRIPTOR)).build());
    bazPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, bazPlugin, barPlugin);
  }

  @Test
  public void resolvesMultipleDependencies() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(bazPlugin, barPlugin, fooPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(BAZ_PLUGIN_DESCRIPTOR)).build());
    bazPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, bazPlugin, barPlugin);
  }

  @Test
  public void resolvesPluginWithNewestVersionOnDependency() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(barPlugin, bazPlugin, fooPlugin);
    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(LATEST_BAZ_PLUGIN_DESCRIPTOR)).build());
    bazPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());
    latestBazPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR)).build());

    when(artifactDescriptorFactory
        .create(argThat(hasProperty("absolutePath", endsWith(latestBazPlugin.getBundleDescriptor().getArtifactFileName()))),
                any(Optional.class))).thenReturn(latestBazPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, bazPlugin, barPlugin);
  }

  @Test
  public void resolvesDependenciesTwoVersionWhenLatestComesFromTransitiveMinor() throws Exception {
    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, latestEchoPlugin, bazPlugin);
    latestEchoPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(BAZ_PLUGIN_DESCRIPTOR)).build());
    fooPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(ECHO_PLUGIN_DESCRIPTOR)).build());
    echoPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(LATEST_BAZ_PLUGIN_DESCRIPTOR)).build());

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, bazPlugin, latestEchoPlugin, fooPlugin);
  }

  @Test
  public void sanitizesDependantPluginExportedPackages() throws Exception {
    fooPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().exportingPackages(getFooExportedPackages()).build());

    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().dependingOn(singleton(FOO_PLUGIN_DESCRIPTOR))
        .exportingPackages(getBarExportedPackages()).build());

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

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

    final List<ArtifactPluginDescriptor> resolvedPluginDescriptors = dependenciesResolver.resolve(
                                                                                                  emptySet(),
                                                                                                  pluginDescriptors, true);

    assertResolvedPlugins(resolvedPluginDescriptors, fooPlugin, bazPlugin, barPlugin);
    assertPluginExportedPackages(fooPlugin, "org.foo", "org.foo.mule");
    assertPluginExportedPackages(bazPlugin, "org.baz");
    assertPluginExportedPackages(barPlugin, "org.bar", "org.bar.mule");
  }

  @Test
  public void detectsDuplicateExportedPackagesOnIndependentPlugins() throws Exception {
    fooPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().exportingPackages(getFooExportedPackages()).build());

    barPlugin.setClassLoaderModel(new ClassLoaderModelBuilder().exportingPackages(getBarExportedPackages()).build());

    final List<ArtifactPluginDescriptor> pluginDescriptors = createPluginDescriptors(fooPlugin, barPlugin);

    Map<String, List<String>> pluginsPerPackage = new HashMap<>();
    pluginsPerPackage.put("org.foo", asList("bar, foo"));
    pluginsPerPackage.put("org.foo.mule", asList("bar, foo"));
    String expectedErrorMessage = new DuplicateExportedPackageException(pluginsPerPackage).getMessage();

    this.expectedException.expect(DuplicateExportedPackageException.class);
    this.expectedException.expectMessage(expectedErrorMessage);
    dependenciesResolver.resolve(emptySet(), pluginDescriptors, true);
  }

  @Test
  public void providedPluginsHaveOldestVersionOfSamePluginInPolicy() {
    dependenciesResolver.resolve(ImmutableSet.of(bazPlugin), ImmutableList.of(latestBazPlugin), false);
  }

  @Test
  public void providedPluginsHaveOldestVersionOfSamePluginInDomain() {
    expectedException.expect(IllegalStateException.class);
    dependenciesResolver.resolve(ImmutableSet.of(bazPlugin), ImmutableList.of(latestBazPlugin), true);
  }

  @Test
  public void providedPluginsHaveOldestVersionOfSameHttpPluginInPolicy() {
    BundleDescriptor httpDescriptor1_0 = new BundleDescriptor.Builder()
        .setGroupId(MULE_HTTP_CONNECTOR_GROUP_ID)
        .setArtifactId(MULE_HTTP_CONNECTOR_ARTIFACT_ID)
        .setVersion("1.0.0")
        .build();
    BundleDescriptor httpDescriptor1_1 = new BundleDescriptor.Builder()
        .setGroupId(MULE_HTTP_CONNECTOR_GROUP_ID)
        .setArtifactId(MULE_HTTP_CONNECTOR_ARTIFACT_ID)
        .setVersion("1.1.0")
        .build();
    ArtifactPluginDescriptor httpPluginDescriptor1_0 = newArtifactPluginDescriptor("HTTP");
    httpPluginDescriptor1_0.setBundleDescriptor(httpDescriptor1_0);
    ArtifactPluginDescriptor httpPluginDescriptor1_1 = newArtifactPluginDescriptor("HTTP");
    httpPluginDescriptor1_1.setBundleDescriptor(httpDescriptor1_1);

    expectedException.expect(IllegalStateException.class);
    expectedException
        .expectMessage("Incompatible version of plugin 'HTTP' (org.mule.connectors:mule-http-connector) found. Artifact requires version '1.1.0' but context provides version '1.0.0'");
    dependenciesResolver.resolve(ImmutableSet.of(httpPluginDescriptor1_0), ImmutableList.of(httpPluginDescriptor1_1), false);
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

    assertThat(resolvedPluginDescriptors, contains(expectedPluginDescriptors));
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
