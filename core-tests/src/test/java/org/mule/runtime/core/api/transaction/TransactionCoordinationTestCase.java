/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TransactionCoordinationTestCase extends AbstractMuleTestCase {

  private MuleContext muleContext;

  private TransactionCoordination tc;

  @Before
  public void setUpTransaction() throws Exception {
    tc = TransactionCoordination.getInstance();
    muleContext = mockContextWithServices();
    when(muleContext.getConfiguration().getId()).thenReturn("appName");
  }

  @After
  public void unbindTransaction() throws Exception {
    tc.unbindTransaction(tc.getTransaction());
  }

  @Test
  public void testBindTransaction() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    Transaction tx = mock(Transaction.class);

    tc.bindTransaction(tx);
    assertEquals(tx, tc.getTransaction());
    tc.unbindTransaction(tx);
  }

  @Test
  public void testBindTransactionWithAlreadyBound() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    Transaction tx = mock(Transaction.class);

    tc.bindTransaction(tx);
    assertEquals(tx, tc.getTransaction());

    try {
      Transaction tx2 = mock(Transaction.class);
      tc.bindTransaction(tx2);
      fail();
    } catch (IllegalTransactionStateException e) {
      // expected
    }

    tc.unbindTransaction(tx);
  }

  @Test
  public void testUnbindTransactionWithoutBound() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    Transaction tx = mock(Transaction.class);

    tc.unbindTransaction(tx);
  }

  @Test
  public void testSetInstanceWithBound() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    Transaction tx = mock(Transaction.class);

    tc.bindTransaction(tx);

    tc.unbindTransaction(tx);
  }

  @Test
  public void testCommitCurrentTransaction() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    tc.commitCurrentTransaction();
    TestTransaction testTransaction = spy(new TestTransaction(muleContext));

    tc.bindTransaction(testTransaction);
    tc.commitCurrentTransaction();
    assertThat(tc.getTransaction(), nullValue());
    verify(testTransaction, times(1)).commit();
  }

  @Test
  public void testCommitCurrentTransactionWithSuspendedTransaction() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    TestTransaction xaTx = spy(new TestTransaction(muleContext));
    xaTx.setXA(true);
    Transaction tx = spy(new TestTransaction(muleContext));

    tc.bindTransaction(xaTx);
    tc.suspendCurrentTransaction();
    tc.bindTransaction(tx);
    tc.commitCurrentTransaction();
    tc.resumeSuspendedTransaction();

    assertThat((TestTransaction) tc.getTransaction(), is(xaTx));
    verify(xaTx, times(1)).suspend();
    verify(xaTx, times(1)).resume();
    verify(tx, times(1)).commit();
  }

  @Test
  public void testCommitDoesntFailOnException() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    Transaction tx = mock(Transaction.class);
    doThrow(new TransactionException((Throwable) null)).when(tx).commit();
    TransactionCoordination.getInstance().commitCurrentTransaction();
  }

  @Test
  public void testRollbackCurrentTransaction() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    tc.commitCurrentTransaction();
    TestTransaction testTransaction = spy(new TestTransaction(muleContext));

    tc.bindTransaction(testTransaction);
    tc.rollbackCurrentTransaction();
    assertThat(tc.getTransaction(), nullValue());
    verify(testTransaction, times(1)).rollback();
  }

  @Test
  public void testRollbackCurrentTransactionWithSuspendedTransaction() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    TestTransaction xaTx = spy(new TestTransaction(muleContext));
    xaTx.setXA(true);
    Transaction tx = spy(new TestTransaction(muleContext));

    tc.bindTransaction(xaTx);
    tc.suspendCurrentTransaction();
    tc.bindTransaction(tx);
    tc.rollbackCurrentTransaction();
    tc.resumeSuspendedTransaction();

    assertThat((TestTransaction) tc.getTransaction(), is(xaTx));
    verify(xaTx, times(1)).suspend();
    verify(xaTx, times(1)).resume();
    verify(tx, times(1)).rollback();
  }

  @Test
  public void testRollbackDoesntFailOnException() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    Transaction tx = mock(Transaction.class);
    doThrow(new TransactionException((Throwable) null)).when(tx).rollback();
    TransactionCoordination.getInstance().rollbackCurrentTransaction();
  }

  @Test
  public void testSuspendResumeTransaction() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    Transaction tx = mock(Transaction.class);
    tc.bindTransaction(tx);
    tc.suspendCurrentTransaction();
    assertNull(tc.getTransaction());
    tc.resumeSuspendedTransaction();
    verify(tx, times(1)).suspend();
    verify(tx, times(1)).resume();
    assertThat(tc.getTransaction(), is(tx));
  }

  @Test
  public void testResumeXaTransactionIfAvailableWithNoTx() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    tc.resumeXaTransactionIfAvailable();

    Transaction tx = spy(new TestTransaction(muleContext));
    tc.bindTransaction(tx);
    tc.resumeXaTransactionIfAvailable();
    verify(tx, times(0)).resume();
  }

  @Test
  public void testResumeXaTransactionIfAvailableWithTx() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    tc.resumeXaTransactionIfAvailable();

    TestTransaction tx = spy(new TestTransaction(muleContext));
    tx.setXA(true);
    tc.bindTransaction(tx);
    tc.suspendCurrentTransaction();
    tc.resumeXaTransactionIfAvailable();
    verify(tx, times(1)).suspend();
    verify(tx, times(1)).resume();
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testResumeXaTransactionTwice() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    TestTransaction tx = spy(new TestTransaction(muleContext));
    tx.setXA(true);
    tc.bindTransaction(tx);
    tc.resumeSuspendedTransaction();
    tc.resumeSuspendedTransaction();
  }

  @Test
  public void testResolveTransactionForRollback() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    TestTransaction tx = spy(new TestTransaction(muleContext));
    tx.setXA(true);
    tc.bindTransaction(tx);
    tx.setRollbackOnly();
    tc.resolveTransaction();
    assertThat(tc.getTransaction(), nullValue());
    verify(tx, times(1)).rollback();
  }

  @Test
  public void testResolveTransactionForCommit() throws Exception {
    assertThat(tc.getTransaction(), nullValue());
    TestTransaction tx = spy(new TestTransaction(muleContext));
    tx.setXA(true);
    tc.bindTransaction(tx);
    tc.resolveTransaction();
    assertThat(tc.getTransaction(), nullValue());
    verify(tx, times(1)).commit();
  }

}
