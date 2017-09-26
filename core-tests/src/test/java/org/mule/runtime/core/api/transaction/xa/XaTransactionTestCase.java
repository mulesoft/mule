/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction.xa;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transaction.XaTransaction;
import org.mule.runtime.core.privileged.transaction.xa.XaResourceFactoryHolder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class XaTransactionTestCase extends AbstractMuleTestCase {

  private MuleContext mockMuleContext = mockContextWithServices();
  @Mock
  private TransactionManager mockTransactionManager;
  @Mock
  private XaResourceFactoryHolder mockXaResourceFactoryHolder1;
  @Mock
  private XaResourceFactoryHolder mockXaResourceFactoryHolder2;
  @Mock
  private XAResource mockXaResource;

  @Before
  public void setUpMuleContext() throws RegistrationException {
    when(mockMuleContext.getTransactionManager()).thenReturn(mockTransactionManager);
    when(mockMuleContext.getConfiguration().getId()).thenReturn("appName");
  }

  @Test
  public void recognizeDifferentWrappersOfSameResource() throws Exception {
    XaTransaction xaTransaction = new XaTransaction(mockMuleContext);
    Object resourceFactory = new Object();
    Object resource = new Object();
    when(mockXaResourceFactoryHolder1.getHoldObject()).thenReturn(resourceFactory);
    when(mockXaResourceFactoryHolder2.getHoldObject()).thenReturn(resourceFactory);
    xaTransaction.bindResource(mockXaResourceFactoryHolder1, resource);
    assertThat(xaTransaction.hasResource(mockXaResourceFactoryHolder1), is(true));
    assertThat(xaTransaction.hasResource(mockXaResourceFactoryHolder2), is(true));
    assertThat(xaTransaction.getResource(mockXaResourceFactoryHolder2), is(resource));
  }

  @Test
  public void isRollbackOnly() throws Exception {
    javax.transaction.Transaction tx = mock(javax.transaction.Transaction.class);
    when(tx.getStatus()).thenReturn(Transaction.STATUS_ACTIVE).thenReturn(Transaction.STATUS_COMMITTED)
        .thenReturn(Transaction.STATUS_MARKED_ROLLBACK).thenReturn(Transaction.STATUS_ROLLEDBACK)
        .thenReturn(Transaction.STATUS_ROLLING_BACK);

    when(mockTransactionManager.getTransaction()).thenReturn(tx);

    XaTransaction xaTransaction = new XaTransaction(mockMuleContext);
    xaTransaction.begin();

    assertFalse(xaTransaction.isRollbackOnly());
    assertFalse(xaTransaction.isRollbackOnly());
    assertTrue(xaTransaction.isRollbackOnly());
    assertTrue(xaTransaction.isRollbackOnly());
    assertTrue(xaTransaction.isRollbackOnly());
  }

  @Test
  public void setTxTimeoutWhenEnlistingResource() throws Exception {
    javax.transaction.Transaction tx = mock(javax.transaction.Transaction.class);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    XaTransaction xaTransaction = new XaTransaction(mockMuleContext);
    int timeoutValue = 1500;
    int timeoutValueInSeconds = 1500 / 1000;
    xaTransaction.setTimeout(timeoutValue);
    xaTransaction.begin();
    xaTransaction.enlistResource(mockXaResource);
    verify(mockXaResource).setTransactionTimeout(timeoutValueInSeconds);
  }

  @Test
  public void setsTransactionTimeoutOnBegin() throws Exception {
    final int timeoutMillis = 5000;
    final int timeoutSecs = timeoutMillis / 1000;

    XaTransaction xaTransaction = new XaTransaction(mockMuleContext);
    xaTransaction.setTimeout(timeoutMillis);
    xaTransaction.begin();

    final InOrder inOrder = inOrder(mockTransactionManager);
    inOrder.verify(mockTransactionManager).setTransactionTimeout(timeoutSecs);
    inOrder.verify(mockTransactionManager).begin();
  }
}
