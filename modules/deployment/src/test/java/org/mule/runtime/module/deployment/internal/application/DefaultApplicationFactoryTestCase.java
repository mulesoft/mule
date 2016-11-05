/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.application.ApplicationClassLoaderBuilder;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.deployment.internal.domain.DomainRepository;
import org.mule.runtime.module.service.ServiceRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DefaultApplicationFactoryTestCase extends AbstractMuleTestCase {

  private static final String DOMAIN_NAME = "test-domain";
  private static final String APP_NAME = "test-app";
  private static final String FAKE_ARTIFACT_PLUGIN = "fake-artifact-plugin";
  public static final String APP_ID = "test-app-id";

  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
      mock(ApplicationClassLoaderBuilderFactory.class);
  private final DomainRepository domainRepository = mock(DomainRepository.class);
  private final ArtifactPluginRepository applicationPluginRepository = mock(ArtifactPluginRepository.class);
  private final ApplicationDescriptorFactory applicationDescriptorFactory = mock(ApplicationDescriptorFactory.class);
  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);
  private final DefaultApplicationFactory applicationFactory =
      new DefaultApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                    applicationPluginRepository, domainRepository, serviceRepository, null);

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createsApplication() throws Exception {
    final ApplicationDescriptor descriptor = new ApplicationDescriptor(APP_NAME);
    descriptor.setDomain(DOMAIN_NAME);
    final File[] resourceFiles = new File[0];
    descriptor.setConfigResourcesFile(resourceFiles);
    when(applicationDescriptorFactory.create(any())).thenReturn(descriptor);

    final ArtifactPluginDescriptor coreArtifactPluginDescriptor = mock(ArtifactPluginDescriptor.class);
    List<ArtifactPluginDescriptor> containerArtifactPluginDescriptors = new LinkedList<>();
    containerArtifactPluginDescriptors.add(coreArtifactPluginDescriptor);
    when(applicationPluginRepository.getContainerArtifactPluginDescriptors())
        .thenReturn(containerArtifactPluginDescriptors);

    final ArtifactPlugin appPlugin = mock(ArtifactPlugin.class);
    final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    when(appPlugin.getArtifactClassLoader()).thenReturn(artifactClassLoader);
    when(artifactClassLoader.getArtifactId()).thenReturn(FAKE_ARTIFACT_PLUGIN);
    when(coreArtifactPluginDescriptor.getClassLoaderModel()).thenReturn(mock(ClassLoaderModel.class));
    when(coreArtifactPluginDescriptor.getName()).thenReturn(FAKE_ARTIFACT_PLUGIN);
    when(appPlugin.getDescriptor()).thenReturn(coreArtifactPluginDescriptor);

    final Domain domain = createDomain(DOMAIN_NAME);

    final ClassLoaderLookupPolicy sharedLibLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(domain.getArtifactClassLoader().getClassLoaderLookupPolicy().extend(anyMap()))
        .thenReturn(sharedLibLookupPolicy);

    final MuleApplicationClassLoader applicationArtifactClassLoader = mock(MuleApplicationClassLoader.class);
    when(applicationArtifactClassLoader.getArtifactId()).thenReturn(APP_ID);

    ApplicationClassLoaderBuilder applicationClassLoaderBuilderMock = mock(ApplicationClassLoaderBuilder.class);
    when(applicationClassLoaderBuilderMock.setDomain(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.setArtifactDescriptor(any()))
        .thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.setArtifactId(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock
        .addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0])))
            .thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.build()).thenReturn(applicationArtifactClassLoader);
    when(applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder())
        .thenReturn(applicationClassLoaderBuilderMock);

    List<ArtifactClassLoader> pluginClassLoaders = new ArrayList<>();
    pluginClassLoaders.add(artifactClassLoader);
    when(applicationArtifactClassLoader.getArtifactPluginClassLoaders()).thenReturn(pluginClassLoaders);

    final Application application = applicationFactory.createArtifact(new File(APP_NAME));

    assertThat(application.getDomain(), is(domain));
    assertThat(application.getArtifactClassLoader(), is(applicationArtifactClassLoader));
    assertThat(application.getDescriptor(), is(descriptor));
    assertThat(application.getArtifactName(), is(APP_NAME));
    assertThat(application.getResourceFiles(), is(resourceFiles));

    verify(domainRepository, times(2)).getDomain(DOMAIN_NAME);
    verify(applicationClassLoaderBuilderMock).setDomain(domain);
    verify(applicationClassLoaderBuilderMock)
        .addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0]));
    verify(applicationClassLoaderBuilderMock).setArtifactDescriptor(descriptor);
    verify(applicationClassLoaderBuilderMock).setArtifactId(APP_NAME);
  }

  private Domain createDomain(String name) {
    final Domain domain = mock(Domain.class);
    final ArtifactClassLoader domainArtifactClassLoader = mock(ArtifactClassLoader.class);
    when(domainArtifactClassLoader.getClassLoader()).thenReturn(mock(ClassLoader.class));

    final ClassLoaderLookupPolicy domainLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(domainArtifactClassLoader.getClassLoaderLookupPolicy()).thenReturn(domainLookupPolicy);
    when(domainRepository.getDomain(name)).thenReturn(domain);
    when(domain.getArtifactClassLoader()).thenReturn(domainArtifactClassLoader);

    return domain;
  }

  @Test
  public void applicationDesployFailDueToDomainNotDeployed() throws Exception {
    final ApplicationDescriptor descriptor = new ApplicationDescriptor(APP_NAME);
    descriptor.setDomain(DOMAIN_NAME);
    when(applicationDescriptorFactory.create(any())).thenReturn(descriptor);

    expectedException.expect(DeploymentException.class);
    applicationFactory.createArtifact(new File(APP_NAME));
  }
}
