/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.launcher.ApplicationClassLoaderBuilder;
import org.mule.runtime.module.launcher.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.DeploymentException;
import org.mule.runtime.module.launcher.MuleApplicationClassLoader;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;
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

  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
      mock(ApplicationClassLoaderBuilderFactory.class);
  private final DomainRepository domainRepository = mock(DomainRepository.class);
  private final ArtifactPluginRepository applicationPluginRepository = mock(ArtifactPluginRepository.class);
  private final ApplicationDescriptorFactory applicationDescriptorFactory = mock(ApplicationDescriptorFactory.class);
  private final ArtifactPluginFactory artifactPluginFactory = mock(ArtifactPluginFactory.class);
  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);
  private final DefaultApplicationFactory applicationFactory =
      new DefaultApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                    applicationPluginRepository, domainRepository, serviceRepository);

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createsApplication() throws Exception {
    final ApplicationDescriptor descriptor = new ApplicationDescriptor();
    descriptor.setName(APP_NAME);
    descriptor.setDomain(DOMAIN_NAME);
    final File[] resourceFiles = new File[0];
    descriptor.setConfigResourcesFile(resourceFiles);
    when(applicationDescriptorFactory.create(any())).thenReturn(descriptor);

    final ArtifactPluginDescriptor coreArtifactPluginDescriptor = mock(ArtifactPluginDescriptor.class);
    List<ArtifactPluginDescriptor> containerArtifactPluginDescriptors = new LinkedList<>();
    containerArtifactPluginDescriptors.add(coreArtifactPluginDescriptor);
    when(applicationPluginRepository.getContainerArtifactPluginDescriptors()).thenReturn(containerArtifactPluginDescriptors);

    final ArtifactPlugin appPlugin = mock(ArtifactPlugin.class);
    final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    when(appPlugin.getArtifactClassLoader()).thenReturn(artifactClassLoader);
    when(artifactClassLoader.getArtifactName()).thenReturn(FAKE_ARTIFACT_PLUGIN);
    final ArtifactClassLoaderFilter classLoaderFilter = mock(DefaultArtifactClassLoaderFilter.class);
    when(coreArtifactPluginDescriptor.getClassLoaderFilter()).thenReturn(classLoaderFilter);
    when(coreArtifactPluginDescriptor.getName()).thenReturn(FAKE_ARTIFACT_PLUGIN);
    when(appPlugin.getDescriptor()).thenReturn(coreArtifactPluginDescriptor);
    when(artifactPluginFactory.create(same(coreArtifactPluginDescriptor), any())).thenReturn(appPlugin);

    final Domain domain = createDomain(DOMAIN_NAME);

    final ClassLoaderLookupPolicy sharedLibLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(domain.getArtifactClassLoader().getClassLoaderLookupPolicy().extend(anyMap())).thenReturn(sharedLibLookupPolicy);

    final MuleApplicationClassLoader applicationArtifactClassLoader = mock(MuleApplicationClassLoader.class);

    ApplicationClassLoaderBuilder applicationClassLoaderBuilderMock = mock(ApplicationClassLoaderBuilder.class);
    when(applicationClassLoaderBuilderMock.setDomain(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.setArtifactDescriptor(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.setArtifactId(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock
        .addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0])))
            .thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.build()).thenReturn(applicationArtifactClassLoader);
    when(applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()).thenReturn(applicationClassLoaderBuilderMock);

    List<ArtifactClassLoader> pluginClassLoaders = new ArrayList<>();
    pluginClassLoaders.add(artifactClassLoader);
    when(applicationArtifactClassLoader.getArtifactPluginClassLoaders()).thenReturn(pluginClassLoaders);

    final Application application = applicationFactory.createArtifact(APP_NAME);

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
    final ApplicationDescriptor descriptor = new ApplicationDescriptor();
    descriptor.setName(APP_NAME);
    descriptor.setDomain(DOMAIN_NAME);
    when(applicationDescriptorFactory.create(any())).thenReturn(descriptor);

    expectedException.expect(DeploymentException.class);
    applicationFactory.createArtifact(APP_NAME);
  }
}
