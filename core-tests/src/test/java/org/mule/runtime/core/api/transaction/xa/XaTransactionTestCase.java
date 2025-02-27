/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction.xa;

import static org.mule.runtime.core.api.transaction.Transaction.STATUS_NO_TRANSACTION;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.MuleXaObject;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionStatusException;
import org.mule.runtime.core.internal.transaction.XaTransaction;
import org.mule.runtime.core.internal.transaction.xa.XaResourceFactoryHolder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

@SmallTest
public class XaTransactionTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private final MuleContext mockMuleContext = mockContextWithServices();
  private NotificationDispatcher notificationDispatcher;

  @Mock
  private TransactionManager mockTransactionManager;

  @Mock
  private XaResourceFactoryHolder mockXaResourceFactoryHolder1;

  @Mock
  private XaResourceFactoryHolder mockXaResourceFactoryHolder2;

  @Mock
  private XAResource mockXaResource;

  @Before
  public void setUpMuleContext() throws Exception {
    when(mockMuleContext.getConfiguration().getId()).thenReturn("appName");
    notificationDispatcher = getNotificationDispatcher(mockMuleContext);
  }

  @Test
  public void recognizeDifferentWrappersOfSameResource() throws Exception {
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
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
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    when(tx.getStatus()).thenReturn(Transaction.STATUS_ACTIVE).thenReturn(Transaction.STATUS_COMMITTED)
        .thenReturn(Transaction.STATUS_MARKED_ROLLBACK).thenReturn(Transaction.STATUS_ROLLEDBACK)
        .thenReturn(Transaction.STATUS_ROLLING_BACK);

    when(mockTransactionManager.getTransaction()).thenReturn(tx);

    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    xaTransaction.begin();

    assertThat(xaTransaction.isRollbackOnly(), is(false));
    assertThat(xaTransaction.isRollbackOnly(), is(false));
    assertThat(xaTransaction.isRollbackOnly(), is(true));
    assertThat(xaTransaction.isRollbackOnly(), is(true));
    assertThat(xaTransaction.isRollbackOnly(), is(true));
  }

  @Test
  public void setTxTimeoutWhenEnlistingResource() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    int timeoutValue = 1500;
    int timeoutValueInSeconds = 1500 / 1000;
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    xaTransaction.setTimeout(timeoutValue);
    xaTransaction.begin();
    xaTransaction.enlistResource(mockXaResource);
    verify(mockXaResource).setTransactionTimeout(timeoutValueInSeconds);
  }

  @Test
  public void setsTransactionTimeoutOnBegin() throws Exception {
    final int timeoutMillis = 5000;
    final int timeoutSecs = timeoutMillis / 1000;

    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    xaTransaction.setTimeout(timeoutMillis);
    xaTransaction.begin();

    final InOrder inOrder = inOrder(mockTransactionManager);
    inOrder.verify(mockTransactionManager).setTransactionTimeout(timeoutSecs);
    inOrder.verify(mockTransactionManager).begin();
  }

  @Test(expected = IllegalStateException.class)
  public void beginWithoutTransactionManager() throws TransactionException {
    new XaTransaction("appName", null, notificationDispatcher).begin();
  }

  @Test
  public void commitTransaction() throws Exception {
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    xaTransaction.begin();
    xaTransaction.commit();
    verify(mockTransactionManager).commit();
  }

  @Test
  public void rollbackTransaction() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    xaTransaction.rollback();
    verify(mockTransactionManager).rollback();
  }

  @Test(expected = IllegalStateException.class)
  public void rollbackOnlyError() {
    XaTransaction xaTransaction = new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    xaTransaction.setRollbackOnly();
  }

  @Test
  public void rollbackOnly() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    xaTransaction.setRollbackOnly();
    verify(tx).setRollbackOnly();
  }

  @Test(expected = IllegalStateException.class)
  public void rollbackOnlyFailingInTx() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    doThrow(SystemException.class).when(tx).setRollbackOnly();
    xaTransaction.setRollbackOnly();
  }

  @Test
  public void noTxStatus() throws TransactionStatusException {
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    assertThat(xaTransaction.getStatus(), is(STATUS_NO_TRANSACTION));
  }

  @Test(expected = TransactionStatusException.class)
  public void getStatusFailing() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    when(tx.getStatus()).thenThrow(SystemException.class);
    xaTransaction.getStatus();
  }

  @Test
  public void delistResource() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    XAResource resource = mock(XAResource.class);
    xaTransaction.delistResource(resource, 10);
    verify(tx).delistResource(resource, 10);
  }

  @Test(expected = TransactionException.class)
  public void delistResourceFailing() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    XAResource resource = mock(XAResource.class);
    doThrow(SystemException.class).when(tx).delistResource(resource, 10);
    xaTransaction.delistResource(resource, 10);
  }

  @Test(expected = TransactionException.class)
  public void delistResourceNoTx() throws Exception {
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    XAResource resource = mock(XAResource.class);
    xaTransaction.delistResource(resource, 10);
  }

  @Test
  public void stringTest() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    when(tx.toString()).thenReturn("test");
    xaTransaction.begin();
    assertThat(xaTransaction.toString(), is("test"));
  }

  @Test
  public void isXa() throws Exception {
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    assertThat(xaTransaction.isXA(), is(true));
  }

  @Test(expected = IllegalStateException.class)
  public void resumeWithoutManager() throws Exception {
    XaTransaction xaTransaction = new XaTransaction("appName", null, notificationDispatcher);
    xaTransaction.resume();
  }

  @Test
  public void resume() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    xaTransaction.resume();
    verify(mockTransactionManager).resume(tx);
  }

  @Test(expected = TransactionException.class)
  public void resumeWithError() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    doThrow(SystemException.class).when(mockTransactionManager).resume(any());
    xaTransaction.begin();
    xaTransaction.resume();
  }

  @Test(expected = IllegalStateException.class)
  public void suspendWithoutManager() throws Exception {
    XaTransaction xaTransaction = new XaTransaction("appName", null, notificationDispatcher);
    xaTransaction.suspend();
  }

  @Test
  public void suspend() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    xaTransaction.suspend();
    verify(mockTransactionManager).suspend();
  }

  @Test(expected = TransactionException.class)
  public void suspendWithError() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    when(mockTransactionManager.suspend()).thenThrow(SystemException.class);
    xaTransaction.begin();
    xaTransaction.suspend();
  }

  @Test
  public void bindAndDelist() throws Exception {
    jakarta.transaction.Transaction tx = mock(jakarta.transaction.Transaction.class);
    XaTransaction xaTransaction =
        new XaTransaction("appName", mockTransactionManager, notificationDispatcher);
    when(mockTransactionManager.getTransaction()).thenReturn(tx);
    xaTransaction.begin();
    XAResource resource = mock(XAResource.class);
    MuleXaObject muleResource = mock(MuleXaObject.class);
    when(mockXaResourceFactoryHolder1.getHoldObject()).thenReturn(resource);
    xaTransaction.bindResource(mockXaResourceFactoryHolder1, resource);
    xaTransaction.bindResource(mockXaResourceFactoryHolder1, muleResource);
    xaTransaction.commit();
    verify(muleResource).delist();
    verify(muleResource).close();
  }


}
