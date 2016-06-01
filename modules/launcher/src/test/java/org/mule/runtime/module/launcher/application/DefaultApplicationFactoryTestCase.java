/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.DeploymentException;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DefaultApplicationFactoryTestCase extends AbstractMuleTestCase
{

    private static final String DOMAIN_NAME = "test-domain";
    private static final String APP_NAME = "test-app";

    private final ArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory = mock(ArtifactClassLoaderFactory.class);
    private final DomainRepository domainRepository = mock(DomainRepository.class);
    private final ApplicationPluginRepository applicationPluginRepository = mock(ApplicationPluginRepository.class);
    private final ApplicationDescriptorFactory applicationDescriptorFactory = mock(ApplicationDescriptorFactory.class);
    private final ApplicationPluginFactory applicationPluginFactory = mock(ApplicationPluginFactory.class);
    private final DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, applicationDescriptorFactory, applicationPluginFactory, domainRepository, applicationPluginRepository);

    @Rule
    public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createsApplication() throws Exception
    {
        final ApplicationDescriptor descriptor = new ApplicationDescriptor();
        descriptor.setName(APP_NAME);
        descriptor.setDomain(DOMAIN_NAME);
        final File[] resourceFiles = new File[0];
        descriptor.setConfigResourcesFile(resourceFiles);
        when(applicationDescriptorFactory.create(any())).thenReturn(descriptor);

        final ApplicationPluginDescriptor coreApplicationPluginDescriptor = mock(ApplicationPluginDescriptor.class);
        List<ApplicationPluginDescriptor> containerApplicationPluginDescriptors = new LinkedList<>();
        containerApplicationPluginDescriptors.add(coreApplicationPluginDescriptor);
        when(applicationPluginRepository.getContainerApplicationPluginDescriptors()).thenReturn(containerApplicationPluginDescriptors);

        final ApplicationPlugin appPlugin = mock(ApplicationPlugin.class);
        final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
        when(appPlugin.getArtifactClassLoader()).thenReturn(artifactClassLoader);
        final ArtifactClassLoaderFilter classLoaderFilter = mock(ArtifactClassLoaderFilter.class);
        when(coreApplicationPluginDescriptor.getClassLoaderFilter()).thenReturn(classLoaderFilter);
        when(appPlugin.getDescriptor()).thenReturn(coreApplicationPluginDescriptor);
        when(applicationPluginFactory.create(same(coreApplicationPluginDescriptor), any())).thenReturn(appPlugin);

        final Domain domain = createDomain(DOMAIN_NAME);

        final ClassLoaderLookupPolicy sharedLibLookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(domain.getArtifactClassLoader().getClassLoaderLookupPolicy().extend(anyMap())).thenReturn(sharedLibLookupPolicy);

        final ArtifactClassLoader applicationArtifactClassLoader = mock(ArtifactClassLoader.class);
        when(applicationClassLoaderFactory.create(any(), argThat(equalTo(descriptor)))).thenReturn(applicationArtifactClassLoader);

        final Application application = applicationFactory.createArtifact(APP_NAME);

        assertThat(application.getDomain(), is(domain));
        assertThat(application.getArtifactClassLoader(), is(applicationArtifactClassLoader));
        assertThat(application.getDescriptor(), is(descriptor));
        assertThat(application.getArtifactName(), is(APP_NAME));
        assertThat(application.getResourceFiles(), is(resourceFiles));

        verify(applicationClassLoaderFactory).create(any(), argThat(equalTo(descriptor)));
        verify(domainRepository, times(2)).getDomain(DOMAIN_NAME);
        verify(applicationPluginRepository).getContainerApplicationPluginDescriptors();
        verify(coreApplicationPluginDescriptor).getClassLoaderFilter();
        verify(appPlugin).getArtifactClassLoader();
        verify(appPlugin).getDescriptor();
    }

    private Domain createDomain(String name)
    {
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
    public void applicationDesployFailDueToDomainNotDeployed() throws Exception
    {
        expectedException.expect(DeploymentException.class);

        final ApplicationDescriptor descriptor = new ApplicationDescriptor();
        descriptor.setName(APP_NAME);
        descriptor.setDomain(DOMAIN_NAME);
        when(applicationDescriptorFactory.create(any())).thenReturn(descriptor);

        applicationFactory.createArtifact(APP_NAME);
    }
}