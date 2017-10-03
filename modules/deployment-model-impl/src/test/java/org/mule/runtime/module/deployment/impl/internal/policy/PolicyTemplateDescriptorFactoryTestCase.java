/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.Optional.empty;
import static java.io.File.createTempFile;
import static java.io.File.separator;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.toFile;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory.ARTIFACT_DESCRIPTOR_DOES_NOT_EXISTS_ERROR;
import static org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory.invalidClassLoaderModelIdError;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION;
import static org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel.NULL_CLASSLOADER_MODEL;
import static org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader.FILE_SYSTEM_POLICY_MODEL_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader.LIB_DIR;
import static org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory.invalidBundleDescriptorLoaderIdError;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.VERSION;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePolicyModel.MulePolicyModelBuilder;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.LoaderNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class PolicyTemplateDescriptorFactoryTestCase extends AbstractMuleTestCase {

  private static final String POLICY_NAME = "testPolicy";
  private static final String JAR_FILE_NAME = "test.jar";
  private static final String POLICY_VERSION = "1.0.0";
  private static final String POLICY_GROUP_ID = "org.mule.test";
  private static final String POLICY_CLASSIFIER = "mule-policy";
  private static final String POLICY_ARTIFACT_TYPE = "zip";
  private static final String INVALID_LOADER_ID = "INVALID";

  private static final File echoTestJarFile =
      new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java"))
          .compile(JAR_FILE_NAME);

  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader = mock(ArtifactPluginDescriptorLoader.class);
  private final DescriptorLoaderRepository descriptorLoaderRepository = mock(ServiceRegistryDescriptorLoaderRepository.class);


  private static File getResourceFile(String resource) {
    return toFile(PolicyTemplateDescriptorFactoryTestCase.class.getResource(resource));
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    when(descriptorLoaderRepository.get(FILE_SYSTEM_POLICY_MODEL_LOADER_ID, POLICY, ClassLoaderModelLoader.class))
        .thenReturn(new FileSystemPolicyClassLoaderModelLoader());
    when(descriptorLoaderRepository.get(INVALID_LOADER_ID, POLICY, ClassLoaderModelLoader.class))
        .thenThrow(new LoaderNotFoundException(INVALID_LOADER_ID));
    MavenClientProvider mavenClientProvider = MavenClientProvider.discoverProvider(currentThread().getContextClassLoader());
    when(descriptorLoaderRepository.get(MULE_LOADER_ID, POLICY, ClassLoaderModelLoader.class))
        .thenReturn(new DeployableMavenClassLoaderModelLoader(mavenClientProvider.createMavenClient(newMavenConfigurationBuilder()
            .localMavenRepositoryLocation(mavenClientProvider
                .getLocalRepositorySuppliers().environmentMavenRepositorySupplier().get())
            .build()), mavenClientProvider.getLocalRepositorySuppliers()));

    when(descriptorLoaderRepository.get(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, POLICY, BundleDescriptorLoader.class))
        .thenReturn(new PropertiesBundleDescriptorLoader());
    when(descriptorLoaderRepository.get(INVALID_LOADER_ID, POLICY, BundleDescriptorLoader.class))
        .thenThrow(new LoaderNotFoundException(INVALID_LOADER_ID));
  }

  @Test
  public void verifiesThatPolicyDescriptorIsPresent() throws Exception {
    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).usingLibrary(echoTestJarFile.getAbsolutePath());
    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    PolicyTemplateDescriptorFactory descriptorFactory =
        new PolicyTemplateDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);

    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException.expectMessage(allOf(containsString(ARTIFACT_DESCRIPTOR_DOES_NOT_EXISTS_ERROR),
                                          containsString(MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION)));
    descriptorFactory.create(tempFolder, empty());
  }

  @Test
  public void readsRuntimeLibs() throws Exception {
    MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModelBuilder()
        .setName(POLICY_NAME)
        .setMinMuleVersion("4.0.0")
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createPolicyBundleDescriptorLoader(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));
    mulePolicyModelBuilder
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(FILE_SYSTEM_POLICY_MODEL_LOADER_ID, emptyMap()));

    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).usingLibrary(echoTestJarFile.getAbsolutePath())
        .describedBy(mulePolicyModelBuilder.build());
    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    PolicyTemplateDescriptorFactory descriptorFactory =
        new PolicyTemplateDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);
    PolicyTemplateDescriptor desc = descriptorFactory.create(tempFolder, empty());

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[0]).getPath(), equalTo(tempFolder.toString()));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[1]).getPath(),
               equalTo(new File(tempFolder, LIB_DIR + separator + JAR_FILE_NAME).toString()));
  }

  @Test
  public void assignsBundleDescriptor() throws Exception {
    MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModelBuilder()
        .setName(POLICY_NAME)
        .setMinMuleVersion("4.0.0")
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createPolicyBundleDescriptorLoader(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));


    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).usingLibrary(echoTestJarFile.getAbsolutePath())
        .describedBy(mulePolicyModelBuilder.build());
    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    PolicyTemplateDescriptorFactory descriptorFactory =
        new PolicyTemplateDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);
    PolicyTemplateDescriptor desc = descriptorFactory.create(tempFolder, empty());

    assertThat(desc.getBundleDescriptor().getArtifactId(), equalTo(POLICY_NAME));
    assertThat(desc.getBundleDescriptor().getGroupId(), equalTo(POLICY_GROUP_ID));
    assertThat(desc.getBundleDescriptor().getClassifier().get(), equalTo(POLICY_CLASSIFIER));
    assertThat(desc.getBundleDescriptor().getType(), equalTo(POLICY_ARTIFACT_TYPE));
    assertThat(desc.getBundleDescriptor().getVersion(), equalTo(POLICY_VERSION));
  }

  @Test
  public void readsPlugin() throws Exception {
    MulePolicyModelBuilder policyModelBuilder = new MulePolicyModelBuilder()
        .setName(POLICY_NAME)
        .setMinMuleVersion("4.0.0")
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createPolicyBundleDescriptorLoader(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));

    ArtifactPluginFileBuilder plugin1 = new ArtifactPluginFileBuilder("plugin1");
    ArtifactPluginFileBuilder plugin2 = new ArtifactPluginFileBuilder("plugin2");

    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).describedBy(policyModelBuilder.build())
        .dependingOn(plugin1).dependingOn(plugin2);

    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    final ArtifactPluginDescriptorFactory pluginDescriptorFactory = mock(ArtifactPluginDescriptorFactory.class);

    final PolicyTemplateDescriptorFactory policyTemplateDescriptorFactory =
        new PolicyTemplateDescriptorFactory(new ArtifactPluginDescriptorLoader(pluginDescriptorFactory),
                                            descriptorLoaderRepository);

    final ArtifactPluginDescriptor expectedPluginDescriptor1 = mock(ArtifactPluginDescriptor.class);
    when(expectedPluginDescriptor1.getName()).thenReturn("plugin1");
    when(expectedPluginDescriptor1.getClassLoaderModel()).thenReturn(NULL_CLASSLOADER_MODEL);

    final ArtifactPluginDescriptor expectedPluginDescriptor2 = mock(ArtifactPluginDescriptor.class);
    when(expectedPluginDescriptor2.getName()).thenReturn("plugin2");
    when(expectedPluginDescriptor2.getClassLoaderModel()).thenReturn(NULL_CLASSLOADER_MODEL);
    when(pluginDescriptorFactory.create(any(), any())).thenReturn(expectedPluginDescriptor1)
        .thenReturn(expectedPluginDescriptor2);

    PolicyTemplateDescriptor descriptor = policyTemplateDescriptorFactory.create(tempFolder, empty());

    Set<ArtifactPluginDescriptor> plugins = descriptor.getPlugins();
    assertThat(plugins.size(), equalTo(2));
    assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor1)));
    assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor2)));
  }

  @Test
  public void detectsInvalidClassLoaderModelLoaderId() throws Exception {
    MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModelBuilder()
        .setName(POLICY_NAME)
        .setMinMuleVersion("4.0.0")
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createPolicyBundleDescriptorLoader(PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(INVALID_LOADER_ID, emptyMap()));

    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).describedBy(mulePolicyModelBuilder.build());
    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    PolicyTemplateDescriptorFactory descriptorFactory =
        new PolicyTemplateDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);
    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException
        .expectMessage(invalidClassLoaderModelIdError(tempFolder, mulePolicyModelBuilder.getClassLoaderModelDescriptorLoader()));
    descriptorFactory.create(tempFolder, empty());
  }

  @Test
  public void detectsInvalidBundleDescriptorLoaderId() throws Exception {
    MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModelBuilder()
        .setName(POLICY_NAME)
        .setMinMuleVersion("4.0.0")
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createPolicyBundleDescriptorLoader(INVALID_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(FILE_SYSTEM_POLICY_MODEL_LOADER_ID, emptyMap()));

    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).describedBy(mulePolicyModelBuilder.build());
    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    PolicyTemplateDescriptorFactory descriptorFactory =
        new PolicyTemplateDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);
    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException
        .expectMessage(invalidBundleDescriptorLoaderIdError(tempFolder, mulePolicyModelBuilder.getBundleDescriptorLoader()));
    descriptorFactory.create(tempFolder, empty());
  }

  private MuleArtifactLoaderDescriptor createPolicyBundleDescriptorLoader(String bundleDescriptorLoaderId) {
    Map<String, Object> attributes = new HashMap();
    attributes.put(VERSION, POLICY_VERSION);
    attributes.put(GROUP_ID, POLICY_GROUP_ID);
    attributes.put(ARTIFACT_ID, POLICY_NAME);
    attributes.put(CLASSIFIER, POLICY_CLASSIFIER);
    attributes.put(TYPE, POLICY_ARTIFACT_TYPE);
    return new MuleArtifactLoaderDescriptor(bundleDescriptorLoaderId, attributes);
  }

  private File createTempFolder() throws IOException {
    File tempFolder = createTempFile("tempPolicy", null);
    assertThat(tempFolder.delete(), is(true));
    assertThat(tempFolder.mkdir(), is(true));
    return tempFolder;
  }
}
