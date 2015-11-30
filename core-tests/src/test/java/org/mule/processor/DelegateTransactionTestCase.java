/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;


public class DelegateTransactionTestCase extends AbstractMuleTestCase
{

    private static final int DEFAULT_TX_TIMEOUT = 2;
    private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());

    @Test
    public void defaultTxTimeout()
    {
        when(mockMuleContext.getConfiguration().getDefaultTransactionTimeout()).thenReturn(DEFAULT_TX_TIMEOUT);
        DelegateTransaction delegateTransaction = new DelegateTransaction(mockMuleContext);
        assertThat(delegateTransaction.getTimeout(), is(DEFAULT_TX_TIMEOUT));
    }

    @Test
    public void changeTxTimeout()
    {
        DelegateTransaction delegateTransaction = new DelegateTransaction(mockMuleContext);
        int newTimeout = 10;
        delegateTransaction.setTimeout(newTimeout);
        assertThat(delegateTransaction.getTimeout(), is(newTimeout));
    }
}