/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.artifact;

import static org.mule.runtime.container.api.MuleFoldersUtil.getServiceFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder.builder;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.deployment.meta.MuleServiceContractModel;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptorFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.qameta.allure.Story;

@Story(ARTIFACT_DESCRIPTORS)
public class ServiceDescriptorFactoryTestCase extends AbstractMuleTestCase {

  private static final String SERVICE_NAME = "testService";
  private static final String SERVICE_API_CLASS_NAME = "org.foo.FooServiceProvider";
  private static final String PROVIDER_CLASS_NAME = "org.foo.FooServiceProvider";

  private final DescriptorLoaderRepository descriptorLoaderRepository = mock(DescriptorLoaderRepository.class);

  private final ArtifactDescriptorValidator artifactDescriptorValidator = mock(ArtifactDescriptorValidator.class);
  private ServiceDescriptorFactory serviceDescriptorFactory;

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);

  @Before
  public void setUp() throws Exception {
    BundleDescriptorLoader bundleDescriptorLoaderMock = mock(BundleDescriptorLoader.class);
    when(bundleDescriptorLoaderMock.supportsArtifactType(ArtifactType.SERVICE)).thenReturn(true);
    when(bundleDescriptorLoaderMock.load(Mockito.any(File.class), Mockito.any(Map.class), eq(ArtifactType.SERVICE)))
        .thenReturn(new BundleDescriptor.Builder()
            .setGroupId("mockGroupId")
            .setArtifactId("mockArtifactId")
            .setVersion("1.0.0")
            .setClassifier("mule-service")
            .setType("jar")
            .build());


    when(descriptorLoaderRepository.get(anyString(), ArgumentMatchers.any(), argThat(equalTo(BundleDescriptorLoader.class))))
        .thenReturn(bundleDescriptorLoaderMock);
    when(descriptorLoaderRepository.get(anyString(), ArgumentMatchers.any(),
                                        argThat(equalTo(ClassLoaderConfigurationLoader.class))))
        .thenReturn(mock(ClassLoaderConfigurationLoader.class));

    doNothing().when(artifactDescriptorValidator).validate(ArgumentMatchers.any());

    serviceDescriptorFactory = new ServiceDescriptorFactory(descriptorLoaderRepository, builder());
  }


  @Test
  public void createServiceDescriptor() throws Exception {
    File servicesFolder = getServicesFolder();
    assertThat(servicesFolder.mkdirs(), is(true));

    final ServiceFileBuilder fooService =
        new ServiceFileBuilder(SERVICE_NAME)
            .withServiceProviderClass(PROVIDER_CLASS_NAME)
            .forContract(SERVICE_API_CLASS_NAME);

    unzip(fooService.getArtifactFile(), getServiceFolder(SERVICE_NAME));

    ServiceDescriptor descriptor = serviceDescriptorFactory.create(getServiceFolder(SERVICE_NAME), empty());
    assertThat(descriptor.getName(), equalTo(SERVICE_NAME));
    assertThat(descriptor.getRootFolder(), equalTo(getServiceFolder(SERVICE_NAME)));

    assertThat(descriptor.getContractModels(), hasSize(1));
    MuleServiceContractModel contractModel = descriptor.getContractModels().get(0);

    assertThat(contractModel.getServiceProviderClassName(), equalTo(PROVIDER_CLASS_NAME));
    assertThat(contractModel.getContractClassName(), equalTo(SERVICE_API_CLASS_NAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createServiceDescriptorWithoutArtifactDirectory() {
    serviceDescriptorFactory.create(new File("foo"), empty());
  }
}
