/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class TransactionCoordinationTestCase extends AbstractMuleTestCase
{
    private TransactionCoordination tc;

    @Before
    public void setUpTransaction() throws Exception
    {
        tc = TransactionCoordination.getInstance();
    }

    @After
    public void unbindTransaction() throws Exception
    {
        tc.unbindTransaction(tc.getTransaction());
    }

    @Test
    public void testBindTransaction() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        Transaction tx = mock(Transaction.class);

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());
        tc.unbindTransaction(tx);
    }

    @Test
    public void testBindTransactionWithAlreadyBound() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        Transaction tx = mock(Transaction.class);

        tc.bindTransaction(tx);
        assertEquals(tx, tc.getTransaction());

        try
        {
            Transaction tx2 = mock(Transaction.class);
            tc.bindTransaction(tx2);
            fail();
        }
        catch (IllegalTransactionStateException e)
        {
            // expected
        }

        tc.unbindTransaction(tx);
    }

    @Test
    public void testUnbindTransactionWithoutBound() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        Transaction tx = mock(Transaction.class);

        tc.unbindTransaction(tx);
    }

    @Test
    public void testSetInstanceWithBound() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        Transaction tx = mock(Transaction.class);

        tc.bindTransaction(tx);

        tc.unbindTransaction(tx);
    }

    @Test
    public void testCommitCurrentTransaction() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        tc.commitCurrentTransaction();
        TestTransaction testTransaction = spy(new TestTransaction(mock(MuleContext.class)));

        tc.bindTransaction(testTransaction);
        tc.commitCurrentTransaction();
        assertThat(tc.getTransaction(),IsNull.<Object>nullValue());
        verify(testTransaction, times(1)).commit();
    }

    @Test
    public void testCommitCurrentTransactionWithSuspendedTransaction() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        TestTransaction xaTx = spy(new TestTransaction(mock(MuleContext.class)));
        xaTx.setXA(true);
        Transaction tx = spy(new TestTransaction(mock(MuleContext.class)));

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
    public void testCommitDoesntFailOnException() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        Transaction tx = mock(Transaction.class);
        doThrow(new TransactionException((Throwable) null)).when(tx).commit();
        TransactionCoordination.getInstance().commitCurrentTransaction();
    }

    @Test
    public void testRollbackCurrentTransaction() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        tc.commitCurrentTransaction();
        TestTransaction testTransaction = spy(new TestTransaction(mock(MuleContext.class)));

        tc.bindTransaction(testTransaction);
        tc.rollbackCurrentTransaction();
        assertThat(tc.getTransaction(),IsNull.<Object>nullValue());
        verify(testTransaction, times(1)).rollback();
    }

    @Test
    public void testRollbackCurrentTransactionWithSuspendedTransaction() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        TestTransaction xaTx = spy(new TestTransaction(mock(MuleContext.class)));
        xaTx.setXA(true);
        Transaction tx = spy(new TestTransaction(mock(MuleContext.class)));

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
    public void testRollbackDoesntFailOnException() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        Transaction tx = mock(Transaction.class);
        doThrow(new TransactionException((Throwable) null)).when(tx).rollback();
        TransactionCoordination.getInstance().rollbackCurrentTransaction();
    }

    @Test
    public void testSuspendResumeTransaction() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
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
    public void testResumeXaTransactionIfAvailableWithNoTx() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        tc.resumeXaTransactionIfAvailable();

        Transaction tx = spy(new TestTransaction(mock(MuleContext.class)));
        tc.bindTransaction(tx);
        tc.resumeXaTransactionIfAvailable();
        verify(tx, times(0)).resume();
    }

    @Test
    public void testResumeXaTransactionIfAvailableWithTx() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        tc.resumeXaTransactionIfAvailable();

        TestTransaction tx = spy(new TestTransaction(mock(MuleContext.class)));
        tx.setXA(true);
        tc.bindTransaction(tx);
        tc.suspendCurrentTransaction();
        tc.resumeXaTransactionIfAvailable();
        verify(tx, times(1)).suspend();
        verify(tx, times(1)).resume();
    }

    @Test(expected = IllegalTransactionStateException.class)
    public void testResumeXaTransactionTwice() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        TestTransaction tx = spy(new TestTransaction(mock(MuleContext.class)));
        tx.setXA(true);
        tc.bindTransaction(tx);
        tc.resumeSuspendedTransaction();
        tc.resumeSuspendedTransaction();
    }

    @Test
    public void testResolveTransactionForRollback() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        TestTransaction tx = spy(new TestTransaction(mock(MuleContext.class)));
        tx.setXA(true);
        tc.bindTransaction(tx);
        tx.setRollbackOnly();
        tc.resolveTransaction();
        assertThat(tc.getTransaction(),IsNull.<Object>nullValue());
        verify(tx,times(1)).rollback();
    }

    @Test
    public void testResolveTransactionForCommit() throws Exception
    {
        assertThat(tc.getTransaction(), IsNull.<Object>nullValue());
        TestTransaction tx = spy(new TestTransaction(mock(MuleContext.class)));
        tx.setXA(true);
        tc.bindTransaction(tx);
        tc.resolveTransaction();
        assertThat(tc.getTransaction(),IsNull.<Object>nullValue());
        verify(tx,times(1)).commit();
    }

}
