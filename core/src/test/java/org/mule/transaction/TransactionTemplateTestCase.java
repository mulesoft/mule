/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//SUT = TransactionTemplate - MuleTransactionConfig - partial AbstractSingleResourceTransaction
@RunWith(MockitoJUnitRunner.class)
public class TransactionTemplateTestCase
{
    private static final Object RETURN_VALUE = new Object();
    private MuleContext mockMuleContext = mock(MuleContext.class);
    @Spy
    private TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
    @Spy
    private TestTransaction mockNewTransaction = new TestTransaction(mockMuleContext);
    @Mock
    private ExternalTransactionAwareTransactionFactory mockExternalTransactionFactory;


    @Before
    public void unbindTransaction() throws Exception
    {
        Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
        if (currentTransaction != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
        }
    }

    @Test
    public void testNoConfig() throws Exception
    {
        TransactionTemplate transactionTemplate = new TransactionTemplate(null, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionIndifferentConfig() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_INDIFFERENT);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionNeverAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
    }

    @Test(expected = IllegalTransactionStateException.class)
    public void testActionNeverAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
    }

    @Test
    public void testActionNoneAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
    }

    @Test
    public void testActionNoneAndTxForCommit() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
    }


    @Test
    public void testActionNoneAndTxForRollback() throws Exception
    {
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).rollback();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
    }

    @Test
    public void testActionNoneAndXaTx() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).suspend();
        Mockito.verify(mockTransaction).resume();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
    }

    @Test
    public void testActionNoneAndXaTxAndFailureInCallback() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        try
        {
            transactionTemplate.execute(getFailureTransactionCallback());
        }
        catch (MessagingException e){}
        Mockito.verify(mockTransaction).suspend();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        //TODO fix this issue. resume should be called despite exception
        //Mockito.verify(mockTransaction).resume();
    }

    @Test
    public void testActionNoneAndWithExternalTransactionWithNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        config.setInteractWithExternal(true);
        mockExternalTransactionFactory = mock(ExternalTransactionAwareTransactionFactory.class);
        config.setFactory(mockExternalTransactionFactory);
        when(mockExternalTransactionFactory.joinExternalTransaction(mockMuleContext)).thenReturn(mockTransaction);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionNoneAndWithExternalTransactionWithTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        config.setInteractWithExternal(true);
        mockExternalTransactionFactory = mock(ExternalTransactionAwareTransactionFactory.class);
        config.setFactory(mockExternalTransactionFactory);
        Transaction externalTransaction = mock(Transaction.class);
        when(mockExternalTransactionFactory.joinExternalTransaction(mockMuleContext)).thenReturn(externalTransaction);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndCommitTxAndCommitNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).commit();
        Mockito.verify(mockNewTransaction).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        Mockito.verify(mockNewTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndRollbackTxAndCommitNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).rollback();
        Mockito.verify(mockNewTransaction).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockNewTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndRollbackTxAndRollbackNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).rollback();
        Mockito.verify(mockNewTransaction).rollback();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockNewTransaction,VerificationModeFactory.times(0)).commit();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndSuspendXaTxAndCommitNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        mockTransaction.setXA(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockNewTransaction).commit();
        Mockito.verify(mockNewTransaction,VerificationModeFactory.times(0)).rollback();
        Mockito.verify(mockTransaction).suspend();
        Mockito.verify(mockTransaction).resume();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionAlwaysBeginAndSuspendXaTxAndRollbackNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        mockTransaction.setXA(true);
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockNewTransaction).rollback();
        Mockito.verify(mockNewTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction).suspend();
        Mockito.verify(mockTransaction).resume();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionAlwaysBeginAndSuspendXaTxAndFailureCallback() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        mockTransaction.setXA(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        try
        {
            transactionTemplate.execute(getFailureTransactionCallback());
        } catch (MessagingException e) {}
        Mockito.verify(mockTransaction).suspend();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        //TODO fix this issue. New transaction should be resolved - Xa transaction should be resumed - Xa transaction should be in TransactionCoordinator
        //Mockito.verify(mockNewTransaction).rollback();
        //Mockito.verify(mockTransaction).resume();
        //assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test(expected = IllegalTransactionStateException.class)
    public void testActionAlwaysJoinAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
    }

    @Test
    public void testActionAlwaysJoinAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionAlwaysJoinAndExternalTxAndFailureCallback() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
        config.setInteractWithExternal(true);
        mockExternalTransactionFactory = mock(ExternalTransactionAwareTransactionFactory.class);
        config.setFactory(mockExternalTransactionFactory);
        when(mockExternalTransactionFactory.joinExternalTransaction(mockMuleContext)).thenAnswer(new Answer<Transaction>()
        {
            @Override
            public Transaction answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                TransactionCoordination.getInstance().bindTransaction(mockTransaction);
                return mockTransaction;
            }
        });
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        try
        {
            transactionTemplate.execute(getFailureTransactionCallback());
        } catch (MessagingException e) {}
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        //TODO fix this, transaction should be unbinded
        //assertThat( TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionBeginOrJoinAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionBeginOrJoinAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionJoinIfPossibleAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionJoinIfPossibleAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        TransactionTemplate transactionTemplate = new TransactionTemplate(config, mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        Mockito.verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), Is.is(mockTransaction));
    }

    private TransactionCallback getEmptyTransactionCallback(final Object returnObject)
    {
        return new TransactionCallback() {
            @Override
            public Object doInTransaction() throws Exception
            {
                return returnObject;
            }
        };
    }

    private TransactionCallback getRollbackTransactionCallback(final Object returnObject)
    {
        return new TransactionCallback() {
            @Override
            public Object doInTransaction() throws Exception
            {
                TransactionCoordination.getInstance().getTransaction().setRollbackOnly();
                return returnObject;
            }
        };
    }

    private TransactionCallback getFailureTransactionCallback()
    {
        return new TransactionCallback() {
            @Override
            public Object doInTransaction() throws Exception
            {
                throw new MessagingException(null, new RuntimeException());
            }
        };
    }


}
