/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DelegateOnlyLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;

public class DefaultRegionPluginClassLoadersFactoryTestCase extends AbstractMuleTestCase {

  private static final String REGION_ID = "regionId";
  private static final String PRIVILEGED_PACKAGE = "org.foo.privileged";
  private static final String GROUP_ID = "org.mule.test";
  private static final String PLUGIN_ID2 = "plugin2";
  private static final String PLUGIN_ARTIFACT_ID2 = GROUP_ID + ":" + PLUGIN_ID2;
  private static final String PLUGIN_ID1 = "plugin1";
  private static final String PLUGIN_ARTIFACT_ID1 = GROUP_ID + ":" + PLUGIN_ID1;
  private static final BundleDescriptor PLUGIN1_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID1)
          .setVersion("1.0").setClassifier("mule-plugin").build();
  private static final BundleDescriptor PLUGIN2_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID2)
          .setVersion("1.0").setClassifier("mule-plugin").build();

  private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
  private final ArtifactClassLoader containerClassLoader = createContainerClassLoader(moduleRepository);
  private final DefaultRegionPluginClassLoadersFactory factory =
      new DefaultRegionPluginClassLoadersFactory(new DefaultArtifactClassLoaderResolver(containerClassLoader,
                                                                                        moduleRepository, null));
  private final ClassLoaderLookupPolicy regionOwnerLookupPolicy = mock(ClassLoaderLookupPolicy.class);
  private MuleDeployableArtifactClassLoader artifactClassLoader;
  private RegionClassLoader regionClassLoader;

  private final ArtifactPluginDescriptor plugin1Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID1);
  private final ArtifactPluginDescriptor plugin2Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID2);

  private final ArtifactClassLoader pluginClassLoader1 = mock(ArtifactClassLoader.class);
  private final ArtifactClassLoader pluginClassLoader2 = mock(ArtifactClassLoader.class);

  private final ClassLoaderLookupPolicy pluginLookupPolicy = mock(ClassLoaderLookupPolicy.class);

  @Before
  public void setUp() throws Exception {
    regionClassLoader = new RegionClassLoader(REGION_ID, mock(ArtifactDescriptor.class), this.getClass().getClassLoader(),
                                              regionOwnerLookupPolicy);

    artifactClassLoader = new MuleDeployableArtifactClassLoader(REGION_ID, mock(ArtifactDescriptor.class),
                                                                new URL[0], regionClassLoader,
                                                                regionOwnerLookupPolicy);

    regionClassLoader.addClassLoader(artifactClassLoader, mock(ArtifactClassLoaderFilter.class));

    plugin1Descriptor.setBundleDescriptor(PLUGIN1_BUNDLE_DESCRIPTOR);
    plugin2Descriptor.setBundleDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR);

    when(pluginClassLoader1.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(pluginClassLoader1.getArtifactDescriptor()).thenReturn(plugin1Descriptor);

    when(pluginClassLoader2.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(pluginClassLoader2.getArtifactDescriptor()).thenReturn(plugin2Descriptor);
  }

  @Test
  public void createsNoPlugins() {
    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, emptyList(), regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders, empty());
  }

  @Test
  public void createsSinglePlugin() {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = singletonList(plugin1Descriptor);

    ClassLoaderLookupPolicy pluginBaseLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(regionOwnerLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class))))
        .thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Map.class)))).thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class)), eq(true)))
        .thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders.stream().map(ArtifactClassLoader::getArtifactDescriptor).collect(toList()),
               contains((ArtifactDescriptor) pluginClassLoader1.getArtifactDescriptor()));
  }

  @Test
  public void createsIndependentPlugins() {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
    artifactPluginDescriptors.add(plugin1Descriptor);
    artifactPluginDescriptors.add(plugin2Descriptor);

    ClassLoaderLookupPolicy pluginBaseLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(regionOwnerLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class))))
        .thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Map.class)))).thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class)), eq(true)))
        .thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders.stream().map(ArtifactClassLoader::getArtifactDescriptor).collect(toList()),
               contains(asList(equalTo(pluginClassLoader1.getArtifactDescriptor()),
                               equalTo(pluginClassLoader2.getArtifactDescriptor()))));
  }

  @Test
  public void createsDependantPlugins() {
    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(BundleScope.COMPILE).setDescriptor(
                                                                                                                   PLUGIN1_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin2Descriptor
        .setClassLoaderConfiguration(new ClassLoaderConfigurationBuilder().dependingOn(singleton(pluginDependency)).build());

    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
    artifactPluginDescriptors.add(plugin1Descriptor);
    artifactPluginDescriptors.add(plugin2Descriptor);

    ClassLoaderLookupPolicy pluginBaseLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(regionOwnerLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class))))
        .thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Map.class)))).thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class)), eq(true)))
        .thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders.stream().map(ArtifactClassLoader::getArtifactDescriptor).collect(toList()),
               contains(asList(equalTo(pluginClassLoader1.getArtifactDescriptor()),
                               equalTo(pluginClassLoader2.getArtifactDescriptor()))));
  }

  @Test
  public void createsPluginWithPrivilegedContainerAccess() {
    MuleModule privilegedModule = mock(MuleModule.class);
    when(privilegedModule.getPrivilegedArtifacts()).thenReturn(singleton(PLUGIN_ARTIFACT_ID1));
    when(privilegedModule.getPrivilegedExportedPackages()).thenReturn(singleton(PRIVILEGED_PACKAGE));
    when(moduleRepository.getModules()).thenReturn(singletonList(privilegedModule));

    List<ArtifactPluginDescriptor> artifactPluginDescriptors = singletonList(plugin1Descriptor);

    ClassLoaderLookupPolicy pluginBaseLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    ArgumentCaptor<Map> mapArgumentCaptor = forClass(Map.class);
    when(regionOwnerLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class))))
        .thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(mapArgumentCaptor.capture())).thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class)), eq(true)))
        .thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders.stream().map(ArtifactClassLoader::getArtifactDescriptor).collect(toList()),
               contains((ArtifactDescriptor) pluginClassLoader1.getArtifactDescriptor()));
    Map<String, LookupStrategy> value = mapArgumentCaptor.getValue();
    assertThat(value, hasEntry(equalTo(PRIVILEGED_PACKAGE), instanceOf(ContainerOnlyLookupStrategy.class)));
  }

  @Test
  public void createsPluginWithPrivilegedPluginAccess() {
    ClassLoaderConfiguration plugin1ClassLoaderConfiguration = new ClassLoaderConfigurationBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID2)).build();
    plugin1Descriptor.setClassLoaderConfiguration(plugin1ClassLoaderConfiguration);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(BundleScope.COMPILE).setDescriptor(
                                                                                                                   PLUGIN1_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin2Descriptor
        .setClassLoaderConfiguration(new ClassLoaderConfigurationBuilder().dependingOn(singleton(pluginDependency)).build());

    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
    artifactPluginDescriptors.add(plugin1Descriptor);
    artifactPluginDescriptors.add(plugin2Descriptor);

    ClassLoaderLookupPolicy pluginBaseLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    ArgumentCaptor<Map> mapArgumentCaptor = forClass(Map.class);
    when(regionOwnerLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class))))
        .thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(mapArgumentCaptor.capture())).thenReturn(pluginBaseLookupPolicy);
    when(pluginBaseLookupPolicy.extend(argThat(any(Stream.class)), argThat(any(LookupStrategy.class)), eq(true)))
        .thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders.stream().map(ArtifactClassLoader::getArtifactDescriptor).collect(toList()),
               contains(asList(equalTo(pluginClassLoader1.getArtifactDescriptor()),
                               equalTo(pluginClassLoader2.getArtifactDescriptor()))));
    assertThat((Map<String, LookupStrategy>) mapArgumentCaptor.getAllValues().get(0),
               not(hasEntry(equalTo(PRIVILEGED_PACKAGE), instanceOf(DelegateOnlyLookupStrategy.class))));
    assertThat((Map<String, LookupStrategy>) mapArgumentCaptor.getAllValues().get(1),
               hasEntry(equalTo(PRIVILEGED_PACKAGE), instanceOf(DelegateOnlyLookupStrategy.class)));
  }
}
