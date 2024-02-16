/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class BeginAndResolveTransactionInterceptorTestCase extends AbstractMuleTestCase {

  @Mock
  ExecutionInterceptor executionInterceptor;

  @Mock
  TransactionConfig transactionConfig;

  @Mock
  ExecutionCallback executionCallback;

  @Mock
  ExecutionContext executionContext;

  @Mock
  TransactionFactory transactionFactory;

  @Mock
  MessagingException messagingException;

  BeginAndResolveTransactionInterceptor beginAndResolveTransactionInterceptor;

  @Before
  public void before() {
    beginAndResolveTransactionInterceptor =
        new BeginAndResolveTransactionInterceptor(executionInterceptor, transactionConfig, "APP", null, null, null, true, true);
  }

  @Test
  @Issue("MULE-19418")
  public void executeWithException() throws Exception {
    Transaction tx = spy(Transaction.class);

    when(transactionConfig.getAction()).thenReturn(ACTION_ALWAYS_BEGIN);
    when(transactionConfig.getFactory()).thenReturn(transactionFactory);
    when(transactionFactory.beginTransaction(any(), any(), any(), any())).thenReturn(tx);
    when(executionInterceptor.execute(executionCallback, executionContext)).thenThrow(messagingException);

    try {
      TransactionCoordination.getInstance().bindTransaction(tx);
      beginAndResolveTransactionInterceptor.execute(executionCallback, executionContext);
    } catch (MessagingException messagingException1) {
    }
    verify(tx, times(1)).rollback();
  }

}
