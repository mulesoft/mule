/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.domain;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.launcher.domain.Domain.DOMAIN_CONFIG_FILE_LOCATION;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class MuleDomainClassloaderTestCase extends AbstractMuleTestCase
{

    private static final MuleContextFactory muleContextFactory = mock(MuleContextFactory.class);
    private final DomainClassLoaderRepository domainClassLoaderRepository = mock(DomainClassLoaderRepository.class);
    private final DomainDescriptor domainDescriptor = mock(DomainDescriptor.class);
    private final MuleContext context = mock(MuleContext.class);
    private final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    private final ClassLoader originalThreadClassloader = mock(ClassLoader.class);
    private final ClassLoader domainClassloader = mock(ClassLoader.class);
    private URL resource;
    private ClassLoader classloaderUsedInDispose;
    private Domain domain;

    @Before
    public void setUp() throws Exception
    {
        resource = getClass().getClassLoader().getResource("empty-domain-config.xml").toURI().toURL();
        when(domainClassLoaderRepository.getDomainClassLoader(domainDescriptor)).thenReturn(artifactClassLoader);
        when(artifactClassLoader.findLocalResource(DOMAIN_CONFIG_FILE_LOCATION)).thenReturn(resource);
        when(muleContextFactory.createMuleContext(any(List.class), any(MuleContextBuilder.class))).thenReturn(context);
        domain = new TestMuleDomain(domainClassLoaderRepository, domainDescriptor);
        currentThread().setContextClassLoader(originalThreadClassloader);
        doAnswer(new Answer()
        {
            @Override
            public Void answer(InvocationOnMock invocation)
            {
                classloaderUsedInDispose = currentThread().getContextClassLoader();
                return null;
            }
        }).when(context).dispose();
        domain.init();
        when(artifactClassLoader.getClassLoader()).thenReturn(domainClassloader);
    }

    @Test
    public void disposeWithDomainClassloader()
    {
        domain.dispose();

        assertThat(classloaderUsedInDispose, sameInstance(domainClassloader));
        assertThat(currentThread().getContextClassLoader(), is(originalThreadClassloader));
    }

    private static final class TestMuleDomain extends DefaultMuleDomain
    {

        TestMuleDomain(DomainClassLoaderRepository domainClassLoaderRepository, DomainDescriptor descriptor)
        {
            super(domainClassLoaderRepository, descriptor);
        }

        @Override
        protected MuleContextFactory getMuleContextFactory()
        {
            return muleContextFactory;
        }
    }

}