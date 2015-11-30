/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.transaction.TransactionManager;

import org.junit.Test;

public class XaTransactionFactoryTestCase extends AbstractMuleTestCase
{

    @Test
    public void setsTransactionTimeout() throws Exception
    {
        final int timeout = 1000;
        final XaTransactionFactory transactionFactory = new XaTransactionFactory();
        transactionFactory.setTimeout(timeout);

        final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);

        final TransactionManager transactionManager = mock(TransactionManager.class);
        when(muleContext.getTransactionManager()).thenReturn(transactionManager);

        final Transaction transaction = transactionFactory.beginTransaction(muleContext);

        assertThat(transaction.getTimeout(), equalTo(timeout));
    }
}