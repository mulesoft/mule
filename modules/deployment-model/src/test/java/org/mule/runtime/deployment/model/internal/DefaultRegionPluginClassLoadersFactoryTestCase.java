/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DelegateOnlyLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  private final ArtifactClassLoaderFactory pluginClassLoaderFactory = mock(ArtifactClassLoaderFactory.class);
  private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
  private final DefaultRegionPluginClassLoadersFactory factory =
      new DefaultRegionPluginClassLoadersFactory(pluginClassLoaderFactory,
                                                 moduleRepository);
  private final ClassLoaderLookupPolicy regionOwnerLookupPolicy = mock(ClassLoaderLookupPolicy.class);
  private final ArtifactClassLoader regionClassLoader = mock(ArtifactClassLoader.class);

  private final ArtifactPluginDescriptor plugin1Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID1);
  private final ArtifactPluginDescriptor plugin2Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID2);

  private final ArtifactClassLoader pluginClassLoader1 = mock(ArtifactClassLoader.class);
  private final ArtifactClassLoader pluginClassLoader2 = mock(ArtifactClassLoader.class);

  private final ClassLoaderLookupPolicy pluginLookupPolicy = mock(ClassLoaderLookupPolicy.class);

  @Before
  public void setUp() throws Exception {
    when(regionClassLoader.getArtifactId()).thenReturn(REGION_ID);
    when(regionClassLoader.getClassLoader()).thenReturn(this.getClass().getClassLoader());

    String pluginId1 = DefaultRegionPluginClassLoadersFactory.getArtifactPluginId(REGION_ID, PLUGIN_ID1);
    when(pluginClassLoaderFactory.create(pluginId1, plugin1Descriptor, getClass().getClassLoader(), pluginLookupPolicy))
        .thenReturn(pluginClassLoader1);

    String pluginId2 = DefaultRegionPluginClassLoadersFactory.getArtifactPluginId(REGION_ID, PLUGIN_ID2);
    when(pluginClassLoaderFactory.create(pluginId2, plugin2Descriptor, getClass().getClassLoader(), pluginLookupPolicy))
        .thenReturn(pluginClassLoader2);

    plugin1Descriptor.setBundleDescriptor(PLUGIN1_BUNDLE_DESCRIPTOR);
    plugin2Descriptor.setBundleDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR);

    when(pluginClassLoader1.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(pluginClassLoader1.getArtifactDescriptor()).thenReturn(plugin1Descriptor);

    when(pluginClassLoader2.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(pluginClassLoader2.getArtifactDescriptor()).thenReturn(plugin2Descriptor);
  }

  @Test
  public void createsNoPlugins() throws Exception {
    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, emptyList(), regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders, empty());
  }

  @Test
  public void createsSinglePlugin() throws Exception {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = singletonList(plugin1Descriptor);

    when(regionOwnerLookupPolicy.extend(argThat(any(Map.class)))).thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders, contains(pluginClassLoader1));
  }

  @Test
  public void createsIndependentPlugins() throws Exception {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
    artifactPluginDescriptors.add(plugin1Descriptor);
    artifactPluginDescriptors.add(plugin2Descriptor);

    when(regionOwnerLookupPolicy.extend(argThat(any(Map.class)))).thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders, contains(pluginClassLoader1, pluginClassLoader2));
  }

  @Test
  public void createsDependantPlugins() throws Exception {
    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(BundleScope.COMPILE).setDescriptor(
                                                                                                                   PLUGIN1_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin2Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());

    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
    artifactPluginDescriptors.add(plugin1Descriptor);
    artifactPluginDescriptors.add(plugin2Descriptor);

    when(regionOwnerLookupPolicy.extend(argThat(any(Map.class)))).thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders, contains(pluginClassLoader1, pluginClassLoader2));
  }

  @Test
  public void createsPluginWithPrivilegedContainerAccess() throws Exception {
    MuleModule privilegedModule = mock(MuleModule.class);
    when(privilegedModule.getPrivilegedArtifacts()).thenReturn(singleton(PLUGIN_ARTIFACT_ID1));
    when(privilegedModule.getPrivilegedExportedPackages()).thenReturn(singleton(PRIVILEGED_PACKAGE));
    when(moduleRepository.getModules()).thenReturn(singletonList(privilegedModule));

    List<ArtifactPluginDescriptor> artifactPluginDescriptors = singletonList(plugin1Descriptor);

    ArgumentCaptor<Map> mapArgumentCaptor = forClass(Map.class);
    when(regionOwnerLookupPolicy.extend(mapArgumentCaptor.capture())).thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders, contains(pluginClassLoader1));
    Map<String, LookupStrategy> value = mapArgumentCaptor.getValue();
    assertThat(value, hasEntry(equalTo(PRIVILEGED_PACKAGE), instanceOf(ContainerOnlyLookupStrategy.class)));
  }

  @Test
  public void createsPluginWithPrivilegedPluginAccess() throws Exception {
    ClassLoaderModel plugin1ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID2)).build();
    plugin1Descriptor.setClassLoaderModel(plugin1ClassLoaderModel);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(BundleScope.COMPILE).setDescriptor(
                                                                                                                   PLUGIN1_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin2Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());

    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
    artifactPluginDescriptors.add(plugin1Descriptor);
    artifactPluginDescriptors.add(plugin2Descriptor);

    ArgumentCaptor<Map> argumentCaptor = forClass(Map.class);
    when(regionOwnerLookupPolicy.extend(argumentCaptor.capture())).thenReturn(pluginLookupPolicy);

    List<ArtifactClassLoader> pluginClassLoaders =
        factory.createPluginClassLoaders(regionClassLoader, artifactPluginDescriptors, regionOwnerLookupPolicy);

    assertThat(pluginClassLoaders, contains(pluginClassLoader1, pluginClassLoader2));
    assertThat((Map<String, LookupStrategy>) argumentCaptor.getAllValues().get(0),
               not(hasEntry(equalTo(PRIVILEGED_PACKAGE), instanceOf(DelegateOnlyLookupStrategy.class))));
    assertThat((Map<String, LookupStrategy>) argumentCaptor.getAllValues().get(1),
               hasEntry(equalTo(PRIVILEGED_PACKAGE), instanceOf(DelegateOnlyLookupStrategy.class)));
  }
}
