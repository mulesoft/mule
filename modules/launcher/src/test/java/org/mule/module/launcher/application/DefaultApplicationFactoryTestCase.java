/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.module.launcher.ApplicationDescriptorFactory;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.DomainRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultApplicationFactoryTestCase extends AbstractMuleTestCase
{

    private static final String DOMAIN_NAME = "test-domain";
    private static final String APP_NAME = "test-app";

    private final ArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory = mock(ArtifactClassLoaderFactory.class);
    private final DomainRepository domainRepository = mock(DomainRepository.class);
    private final ApplicationDescriptorFactory applicationDescriptorFactory = mock(ApplicationDescriptorFactory.class);
    private final DefaultApplicationFactory applicationFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, applicationDescriptorFactory, domainRepository);

    @Rule
    public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);

    @Test
    public void createsApplication() throws Exception
    {
        final ApplicationDescriptor descriptor = new ApplicationDescriptor();
        descriptor.setName(APP_NAME);
        descriptor.setDomain(DOMAIN_NAME);
        final File[] resourceFiles = new File[0];
        descriptor.setConfigResourcesFile(resourceFiles);
        when(applicationDescriptorFactory.create(any())).thenReturn(descriptor);

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
}