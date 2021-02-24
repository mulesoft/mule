/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.junit.Before;
import org.junit.Test;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.descriptor.DomainDescriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.launcher.domain.Domain.DOMAIN_CONFIG_FILE_LOCATION;

public class DefaultMuleDomainTestCase
{
    private static final MuleContextFactory muleContextFactory = mock(MuleContextFactory.class);
    private DomainDescriptor domainDescriptor;
    private DomainClassLoaderRepository domainClassLoaderRepository;
    private DefaultMuleDomain defaultMuleDomain;
    private MuleContext muleContext;
    private MuleRegistry muleRegistry;

    private URL resource;
    private ArtifactClassLoader artifactClassLoader;

    @Before
    public void setUp() throws Exception
    {
        domainDescriptor = mock(DomainDescriptor.class);
        domainClassLoaderRepository = mock(DomainClassLoaderRepository.class);
        artifactClassLoader = mock(ArtifactClassLoader.class);
        when(domainClassLoaderRepository.getDomainClassLoader(domainDescriptor)).thenReturn(artifactClassLoader);
        when(artifactClassLoader.findLocalResource(anyString())).thenReturn(null);
        resource = getClass().getClassLoader().getResource("empty-domain-config.xml").toURI().toURL();
        when(domainClassLoaderRepository.getDomainClassLoader(domainDescriptor)).thenReturn(artifactClassLoader);
        when(artifactClassLoader.findLocalResource(DOMAIN_CONFIG_FILE_LOCATION)).thenReturn(resource);

        muleContext = mock(DefaultMuleContext.class);
        when(muleContextFactory.createMuleContext(any(List.class), any(MuleContextBuilder.class))).thenReturn(muleContext);

        muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);

        defaultMuleDomain = new TestMuleDomain(domainClassLoaderRepository, domainDescriptor);
        defaultMuleDomain.init();
    }

    @Test
    public void testCancelStartOneConnectorWithRetryPolicy()
    {
        // Given a default mule application with 1 connector that has retryPolicy
        List<Connector> connectors = new ArrayList<>();

        RetryPolicyTemplate retryPolicyTemplate = addMockConnectorWithMockRetryPolicyToList(connectors);
        when(muleRegistry.lookupObjects(Connector.class)).thenReturn(connectors);

        // When cancelling start
        defaultMuleDomain.cancelStart();

        // Connectors's retry policy start is also cancelled
        verify(retryPolicyTemplate).stopRetrying();
    }

    @Test
    public void testCancelStartTwoConnectorsWithRetryPolicies()
    {
        // Given a default mule application with 2 connectors that have retryPolicies
        List<Connector> connectors = new ArrayList<>();

        RetryPolicyTemplate retryPolicyTemplate1 = addMockConnectorWithMockRetryPolicyToList(connectors);
        RetryPolicyTemplate retryPolicyTemplate2 = addMockConnectorWithMockRetryPolicyToList(connectors);

        when(muleRegistry.lookupObjects(Connector.class)).thenReturn(connectors);

        // When cancelling start
        defaultMuleDomain.cancelStart();

        // Then connector's retry policies start is also cancelled
        verify(retryPolicyTemplate1, times(1)).stopRetrying();
        verify(retryPolicyTemplate2, times(1)).stopRetrying();
    }

    @Test
    public void testCancelStartTwoConnectorsWithNullRetryPolicies()
    {
        // Given a default mule application with 2 connectors that have retryPolicies
        List<Connector> connectors = new ArrayList<>();

        Connector connector1 = mock(Connector.class);
        connectors.add(connector1);
        Connector connector2 = mock(Connector.class);
        connectors.add(connector2);

        when(muleRegistry.lookupObjects(Connector.class)).thenReturn(connectors);

        // When cancelling start
        defaultMuleDomain.cancelStart();

        // Then no error occurs

    }

    @Test
    public void testStopCancelStartTwoConnectors()
    {
        // Given a default mule application with 5 connectors that have retryPolicies
        List<Connector> connectors = new ArrayList<>();

        RetryPolicyTemplate retryPolicyTemplate1 = addMockConnectorWithMockRetryPolicyToList(connectors);
        RetryPolicyTemplate retryPolicyTemplate2 = addMockConnectorWithMockRetryPolicyToList(connectors);

        when(muleRegistry.lookupObjects(Connector.class)).thenReturn(connectors);

        // When stopping
        defaultMuleDomain.stop();

        // Then connector's retry policies start is cancelled
        verify(retryPolicyTemplate1, times(1)).stopRetrying();
        verify(retryPolicyTemplate2, times(1)).stopRetrying();
    }

    private RetryPolicyTemplate addMockConnectorWithMockRetryPolicyToList(List<Connector> connectors)
    {
        Connector connector = mock(Connector.class);
        connectors.add(connector);

        RetryPolicyTemplate retryPolicyTemplate = mock(RetryPolicyTemplate.class);
        when(connector.getRetryPolicyTemplate()).thenReturn(retryPolicyTemplate);

        return retryPolicyTemplate;
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