/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.junit.Before;
import org.junit.Test;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.Domain;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultMuleApplicationTestCase
{

    private ApplicationDescriptor applicationDescriptor;
    private ApplicationClassLoaderFactory applicationClassLoaderFactory;
    private Domain domain;
    private DefaultMuleApplication defaultMuleApplication;
    private MuleContext muleContext;
    private MuleRegistry muleRegistry;

    @Before
    public void setUp() throws Exception
    {
        applicationDescriptor = mock(ApplicationDescriptor.class);
        applicationClassLoaderFactory = mock(ApplicationClassLoaderFactory.class);
        domain = mock(Domain.class);

        defaultMuleApplication = new DefaultMuleApplication(applicationDescriptor, applicationClassLoaderFactory, domain);
        muleContext = mock(DefaultMuleContext.class);
        defaultMuleApplication.setMuleContext(muleContext);
        muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
    }

    @Test
    public void testCancelStartOneConnector()
    {
        // Given a default mule application with 1 connector that has retryPolicy
        List<Connector> connectors = new ArrayList<>();

        RetryPolicyTemplate retryPolicyTemplate = addMockConnectorWithMockRetryPolicyToList(connectors);
        when(muleRegistry.lookupObjects(Connector.class)).thenReturn(connectors);

        // When cancelling start
        defaultMuleApplication.cancelStart();

        // Connectors's retry policy start is also cancelled
        verify(retryPolicyTemplate).stopRetrying();
    }

    @Test
    public void testCancelStartTwoConnectors()
    {
        // Given a default mule application with 2 connectors that have retryPolicies
        List<Connector> connectors = new ArrayList<>();

        RetryPolicyTemplate retryPolicyTemplate1 = addMockConnectorWithMockRetryPolicyToList(connectors);
        RetryPolicyTemplate retryPolicyTemplate2 = addMockConnectorWithMockRetryPolicyToList(connectors);

        when(muleRegistry.lookupObjects(Connector.class)).thenReturn(connectors);

        // When cancelling start
        defaultMuleApplication.cancelStart();

        // Then connector's retry policies start is also cancelled
        verify(retryPolicyTemplate1, times(1)).stopRetrying();
        verify(retryPolicyTemplate2, times(1)).stopRetrying();

    }

    @Test
    public void testStopCancelStartTwoConnectors()
    {
        // Given a default mule application with 2 connectors that have retryPolicies
        LifecycleManager lifecycleManager = mock(LifecycleManager.class);
        when(lifecycleManager.isDirectTransition(Stoppable.PHASE_NAME)).thenReturn(true);
        when(muleContext.getLifecycleManager()).thenReturn(lifecycleManager);

        List<Connector> connectors = new ArrayList<>();

        RetryPolicyTemplate retryPolicyTemplate1 = addMockConnectorWithMockRetryPolicyToList(connectors);
        RetryPolicyTemplate retryPolicyTemplate2 = addMockConnectorWithMockRetryPolicyToList(connectors);

        when(muleRegistry.lookupObjects(Connector.class)).thenReturn(connectors);

        // When stopping
        defaultMuleApplication.stop();

        // Then connector's retry policies start is cancelled
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
        defaultMuleApplication.cancelStart();

        // Then no error occurs

    }

    private RetryPolicyTemplate addMockConnectorWithMockRetryPolicyToList(List<Connector> connectors)
    {
        Connector connector = mock(Connector.class);
        connectors.add(connector);

        RetryPolicyTemplate retryPolicyTemplate = mock(RetryPolicyTemplate.class);
        when(connector.getRetryPolicyTemplate()).thenReturn(retryPolicyTemplate);

        return retryPolicyTemplate;
    }
}