/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.transaction.MuleTransactionConfig;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Issue;

@SmallTest
public class BeginAndResolveTransactionInterceptorTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  ExecutionInterceptor executionInterceptor;

  @Mock
  ExecutionCallback executionCallback;

  @Mock
  ExecutionContext executionContext;

  @Mock
  TransactionFactory transactionFactory;

  @Mock
  MessagingException messagingException;

  BeginAndResolveTransactionInterceptor beginAndResolveTransactionInterceptor;

  MuleTransactionConfig transactionConfig;

  @Before
  public void before() {
    transactionConfig = new MuleTransactionConfig();
    beginAndResolveTransactionInterceptor =
        new BeginAndResolveTransactionInterceptor(executionInterceptor, transactionConfig, "APP", null, null, true, true, true);
  }

  @Test
  @Issue("MULE-19418")
  public void executeWithException() throws Exception {
    Transaction tx = spy(Transaction.class);

    transactionConfig.setAction(ACTION_ALWAYS_BEGIN);
    transactionConfig.setFactory(transactionFactory);
    when(transactionFactory.beginTransaction(any(), any(), any())).thenReturn(tx);
    when(executionInterceptor.execute(executionCallback, executionContext)).thenThrow(messagingException);

    try {
      TransactionCoordination.getInstance().bindTransaction(tx);
      beginAndResolveTransactionInterceptor.execute(executionCallback, executionContext);
    } catch (MessagingException messagingException1) {
    }
    verify(tx, times(1)).rollback();
  }

}
