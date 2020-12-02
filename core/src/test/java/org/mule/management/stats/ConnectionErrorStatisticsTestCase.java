/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleContext;
import org.mule.api.context.WorkManager;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicy;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.retry.PolicyStatus;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.retry.policies.AbstractPolicyTemplate;

public class ConnectionErrorStatisticsTestCase
{

    private MuleRegistry muleRegistry;
    private MuleContext muleContext;

    @Before
    public void setUp() throws Exception
    {
        muleContext = mock(MuleContext.class);
        muleRegistry = mock(MuleRegistry.class);
        AllStatistics allStatistics = new TestAllStatistics();
        allStatistics.setEnabled(true);
        when(muleContext.getStatistics()).thenReturn(allStatistics);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
    }

    @Test
    public void connectionErrorsAreComputedAsExecutionErrors() throws Exception
    {
        List<Connector> connectors = new ArrayList<>();

        RetryPolicyTemplate retryPolicyTemplate = addConnectorWithRetryPolicy(connectors);
        RetryCallback callback = mock(RetryCallback.class);
        WorkManager workManager = mock(WorkManager.class);
        doThrow(new Exception("Failure to connect")).when(callback).doWork(any(RetryContext.class));

        try
        {
            retryPolicyTemplate.execute(callback, workManager);
        }
        catch (RetryPolicyExhaustedException e)
        {
            // Nothing to do
        }

        assertThat(muleContext.getStatistics().getApplicationStatistics().getConnectionErrors(), equalTo(1l));
        assertThat(muleContext.getStatistics().getApplicationStatistics().getExecutionErrors(), equalTo(1l));
    }

    private RetryPolicyTemplate addConnectorWithRetryPolicy(List<Connector> connectors)
    {
        Connector connector = mock(Connector.class);
        connectors.add(connector);
        RetryPolicy retryPolicy = mock(RetryPolicy.class);
        PolicyStatus policyStatus = mock(PolicyStatus.class);
        TestRetryPolicyTemplate retryPolicyTemplate = new TestRetryPolicyTemplate(retryPolicy);
        retryPolicyTemplate.setMuleContext(muleContext);

        when(policyStatus.isOk()).thenReturn(false);
        when(retryPolicy.applyPolicy(Mockito.any(Throwable.class))).thenReturn(policyStatus);
        when(connector.getRetryPolicyTemplate()).thenReturn(retryPolicyTemplate);

        return retryPolicyTemplate;
    }

    private static class TestRetryPolicyTemplate extends AbstractPolicyTemplate
    {
        private RetryPolicy retryPolicy;

        public TestRetryPolicyTemplate(RetryPolicy retryPolicy)
        {
            this.retryPolicy = retryPolicy;
        }

        @Override
        public RetryPolicy createRetryInstance()
        {
            return retryPolicy;
        }
    }

    /**
     * An static class to enable the computation of connection errors for testing
     */
    private static class TestAllStatistics extends AllStatistics
    {
        @Override
        public synchronized void add(FlowConstructStatistics stats)
        {
            stats.setComputeConnectionErrors(true);
            super.add(stats);
        }
    }
}