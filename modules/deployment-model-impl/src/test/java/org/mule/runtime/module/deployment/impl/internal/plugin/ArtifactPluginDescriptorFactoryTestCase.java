/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_JSON;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.invalidBundleDescriptorLoaderIdError;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.invalidClassLoaderModelIdError;
import static org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader.FILE_SYSTEM_POLICY_MODEL_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.VERSION;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.LoaderNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ArtifactPluginDescriptorFactoryTestCase extends AbstractMuleTestCase {

  private static final String PLUGIN_NAME = "testPlugin";
  private static final String INVALID_LOADER_ID = "INVALID";
  private static final String MIN_MULE_VERSION = "4.0.0";

  @Rule
  public TemporaryFolder pluginsTempFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final ClassLoaderFilterFactory classLoaderFilterFactory = mock(ClassLoaderFilterFactory.class);
  private final DescriptorLoaderRepository descriptorLoaderRepository = mock(DescriptorLoaderRepository.class);
  private ArtifactPluginDescriptorFactory descriptorFactory = new ArtifactPluginDescriptorFactory(descriptorLoaderRepository);
  private MavenClientProvider mavenClientProvider =
      MavenClientProvider.discoverProvider(ArtifactPluginDescriptorFactoryTestCase.class.getClassLoader());

  @Before
  public void setUp() throws Exception {
    when(classLoaderFilterFactory.create(null, null))
        .thenReturn(NULL_CLASSLOADER_FILTER);

    when(descriptorLoaderRepository.get(MAVEN, PLUGIN, ClassLoaderModelLoader.class))
        .thenReturn(new PluginMavenClassLoaderModelLoader(mavenClientProvider.createMavenClient(
                                                                                                newMavenConfigurationBuilder()
                                                                                                    .withLocalMavenRepositoryLocation(mavenClientProvider
                                                                                                        .getLocalRepositorySuppliers()
                                                                                                        .environmentMavenRepositorySupplier()
                                                                                                        .get())
                                                                                                    .build()),
                                                          mavenClientProvider.getLocalRepositorySuppliers()));

    when(descriptorLoaderRepository.get(FILE_SYSTEM_POLICY_MODEL_LOADER_ID, PLUGIN, ClassLoaderModelLoader.class))
        .thenReturn(new FileSystemPolicyClassLoaderModelLoader());
    when(descriptorLoaderRepository.get(INVALID_LOADER_ID, PLUGIN, ClassLoaderModelLoader.class))
        .thenThrow(new LoaderNotFoundException(INVALID_LOADER_ID));

    when(descriptorLoaderRepository.get(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, PLUGIN, BundleDescriptorLoader.class))
        .thenReturn(new PropertiesBundleDescriptorLoader());
    when(descriptorLoaderRepository.get(INVALID_LOADER_ID, PLUGIN, BundleDescriptorLoader.class))
        .thenThrow(new LoaderNotFoundException(INVALID_LOADER_ID));
  }

  @Test
  public void createPluginDescriptor() throws Exception {
    String pluginName = "samplePlugin";

    MulePluginModel.MulePluginModelBuilder builder = new MulePluginModel.MulePluginModelBuilder();
    builder.withClassLoaderModelDescriber()
        .setId("maven")
        .build();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(GROUP_ID, "org.mule.test");
    attributes.put(ARTIFACT_ID, pluginName);
    attributes.put(VERSION, "1.0.0");
    attributes.put(CLASSIFIER, MULE_PLUGIN_CLASSIFIER);
    attributes.put(TYPE, "jar");

    builder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, attributes));
    MulePluginModel mulePluginModel = builder
        .setName(pluginName)
        .setMinMuleVersion("4.0.0")
        .build();

    ArtifactPluginFileBuilder pluginFileBuilder = new ArtifactPluginFileBuilder(PLUGIN_NAME)
        .tempFolder(pluginsTempFolder.newFolder())
        .describedBy(mulePluginModel);

    final ArtifactPluginDescriptor artifactPluginDescriptor = descriptorFactory.create(pluginFileBuilder.getArtifactFile());
    assertThat(artifactPluginDescriptor.getName(), equalTo(pluginName));
    assertThat(artifactPluginDescriptor.getBundleDescriptor().getArtifactId(), equalTo(pluginName));
  }

  @Test
  public void pluginDescriptorNotFound() throws Exception {
    ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder(PLUGIN_NAME).tempFolder(pluginsTempFolder.newFolder());

    File pluginJarLocation = pluginFileBuilder.getArtifactFile();

    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException.expectMessage(containsString(MULE_PLUGIN_JSON));
    descriptorFactory.create(pluginJarLocation);
  }

  @Test
  public void detectsInvalidClassLoaderModelLoaderId() throws Exception {
    MulePluginModel.MulePluginModelBuilder pluginModelBuilder = new MulePluginModel.MulePluginModelBuilder().setName(PLUGIN_NAME)
        .setMinMuleVersion(MIN_MULE_VERSION)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));
    pluginModelBuilder.withClassLoaderModelDescriber().setId(INVALID_LOADER_ID);

    ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder(PLUGIN_NAME).tempFolder(pluginsTempFolder.newFolder())
            .describedBy(pluginModelBuilder.build());

    File pluginJarLocation = pluginFileBuilder.getArtifactFile();

    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException
        .expectMessage(invalidClassLoaderModelIdError(pluginJarLocation,
                                                      pluginModelBuilder.getClassLoaderModelDescriptorLoader()));

    descriptorFactory.create(pluginJarLocation);
  }

  @Test
  public void detectsInvalidBundleDescriptorModelLoaderId() throws Exception {
    MulePluginModel.MulePluginModelBuilder pluginModelBuilder = new MulePluginModel.MulePluginModelBuilder().setName(PLUGIN_NAME)
        .setMinMuleVersion(MIN_MULE_VERSION).withBundleDescriptorLoader(createBundleDescriptorLoader(INVALID_LOADER_ID));
    pluginModelBuilder.withClassLoaderModelDescriber().setId(FILE_SYSTEM_POLICY_MODEL_LOADER_ID);

    ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder(PLUGIN_NAME).tempFolder(pluginsTempFolder.newFolder())
            .describedBy(pluginModelBuilder.build());

    File pluginJarLocation = pluginFileBuilder.getArtifactFile();

    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException
        .expectMessage(invalidBundleDescriptorLoaderIdError(pluginJarLocation, pluginModelBuilder.getBundleDescriptorLoader()));

    descriptorFactory.create(pluginJarLocation);
  }

  private MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String bundleDescriptorLoaderId) {
    Map<String, Object> attributes = new HashMap();
    attributes.put(VERSION, "1.0");
    attributes.put(GROUP_ID, "org.mule.test");
    attributes.put(ARTIFACT_ID, PLUGIN_NAME);
    attributes.put(CLASSIFIER, MULE_PLUGIN_CLASSIFIER);
    attributes.put(TYPE, "jar");
    return new MuleArtifactLoaderDescriptor(bundleDescriptorLoaderId, attributes);
  }

}
