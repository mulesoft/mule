/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry.policies;

import org.junit.Before;
import org.junit.Test;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AbstractPolicyTemplateTestCase
{

    private AbstractPolicyTemplate abstractPolicyTemplate;
    private RetryPolicy retryPolicy;
    private RetryCallback retryCallback;
    private WorkManager workManager;
    private RetryNotifier retryNotifier;

    @Before
    public void setUp() throws Exception
    {
        retryPolicy = mock(RetryPolicy.class);

        abstractPolicyTemplate = new AbstractPolicyTemplate()
        {
            @Override
            public RetryPolicy createRetryInstance()
            {
                return retryPolicy;
            }
        };

        retryNotifier = mock(RetryNotifier.class);
        abstractPolicyTemplate.setNotifier(retryNotifier);

        retryCallback = mock(RetryCallback.class);

        workManager = mock(WorkManager.class);
    }

    @Test
    public void testCancelStart()
    {
        // Given a Policy based on the abstract policy template with a start that is not yet cancelled
        assertThat(abstractPolicyTemplate.getStopRetrying().get(), equalTo(false));

        // When cancelling start
        abstractPolicyTemplate.stopRetrying();

        // Then cancelStart is set to true
        assertThat(abstractPolicyTemplate.getStopRetrying().get(), equalTo(true));
    }

    @Test
    public void executeCanceledStart() throws Exception
    {
        // Given a Policy based on the abstract policy template that should be executed forever but start is cancelled
        doThrow(new Exception()).when(retryCallback).doWork(any(RetryContext.class));
        when(retryPolicy.applyPolicy(any(Throwable.class))).thenReturn(PolicyStatus.policyOk());
        abstractPolicyTemplate.stopRetrying();

        // When executing
        abstractPolicyTemplate.execute(retryCallback, workManager);

        // Then the policy stops executing
    }

}