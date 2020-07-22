/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry.async;

import org.junit.Before;
import org.junit.Test;
import org.mule.api.retry.RetryPolicyTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class AsynchronousRetryTemplateTestCase
{

    private RetryPolicyTemplate delegate;
    private AsynchronousRetryTemplate asynchronousRetryTemplate;

    @Before
    public void setUp()
    {
        delegate = mock(RetryPolicyTemplate.class);
        asynchronousRetryTemplate = new AsynchronousRetryTemplate(delegate);
    }

    @Test
    public void testCancelStart()
    {
        // Given an asynchronous retry template with a delegate

        // When cancelling start
        asynchronousRetryTemplate.cancelStart();

        // Then
        verify(delegate, times(1)).cancelStart();
    }

}