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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.exception.AlreadyHandledMessagingException;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mule.transaction.TransactionTemplateFactory.createNestedTransactionTemplate;
import static org.mule.transaction.TransactionTemplateTestUtils.*;

@RunWith(MockitoJUnitRunner.class)
public class NestedTransactionTemplateTestCase
{
    private static final Object RETURN_VALUE = new Object();
    protected MuleContext mockMuleContext = mock(MuleContext.class);
    @Spy
    protected TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
    @Spy
    protected TestTransaction mockNewTransaction = new TestTransaction(mockMuleContext);
    @Mock
    protected ExternalTransactionAwareTransactionFactory mockExternalTransactionFactory;
    @Mock
    protected MessagingException mockMessagingException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected MuleEvent mockEvent;
    @Mock
    protected AlreadyHandledMessagingException mockAlreadyHandledMessagingException;


    @Before
    public void unbindTransaction() throws Exception
    {
        Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
        if (currentTransaction != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateNullTransactionConfig()
    {
        createTransactionalTemplate(null);
    }

    @Test
    public void testActionIndifferentConfig() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_INDIFFERENT);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionNeverAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
    }

    @Test(expected = IllegalTransactionStateException.class)
    public void testActionNeverAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
    }

    @Test
    public void testActionNoneAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
    }

    @Test
    public void testActionNoneAndTxForCommit() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
    }


    @Test
    public void testActionNoneAndTxForRollback() throws Exception
    {
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
    }

    @Test
    public void testActionNoneAndXaTx() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).suspend();
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
    }

    @Test
    public void testActionNoneAndXaTxAndFailureInCallback() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        MuleEvent exceptionListenerResult = configureExceptionListenerCall();
        Object result = transactionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
        org.junit.Assert.assertThat((MuleEvent) result, Is.is(exceptionListenerResult));
        verify(mockTransaction).suspend();
        verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
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
        when(mockExternalTransactionFactory.joinExternalTransaction(mockMuleContext)).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                TransactionCoordination.getInstance().bindTransaction(mockTransaction);
                return mockTransaction;
            }
        });
        mockTransaction.setXA(true);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).suspend();
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
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
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndCommitTxAndCommitNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockNewTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndRollbackTxAndCommitNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).rollback();
        verify(mockNewTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndRollbackTxAndRollbackNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).rollback();
        verify(mockNewTransaction).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).commit();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndSuspendXaTxAndCommitNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        mockTransaction.setXA(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockNewTransaction).commit();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).rollback();
        verify(mockTransaction).suspend();
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionAlwaysBeginAndSuspendXaTxAndRollbackNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        mockTransaction.setXA(true);
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockNewTransaction).rollback();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction).suspend();
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionAlwaysBeginAndSuspendXaTxAndFailureCallback() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        mockTransaction.setXA(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        MuleEvent exceptionListenerResult = configureExceptionListenerCall();
        Object result = transactionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
        org.junit.Assert.assertThat((MuleEvent) result, Is.is(exceptionListenerResult));
        verify(mockTransaction).suspend();
        verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        //TODO fix this issue. New transaction should be resolved - Xa transaction should be resumed - Xa transaction should be in TransactionCoordinator
        //Mockito.verify(mockNewTransaction).rollback();
        //Mockito.verify(mockTransaction).resume();
        //assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test(expected = IllegalTransactionStateException.class)
    public void testActionAlwaysJoinAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
    }

    @Test
    public void testActionAlwaysJoinAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getRollbackTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
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
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        MuleEvent exceptionListenerResult = configureExceptionListenerCall();
        Object result = transactionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
        org.junit.Assert.assertThat((MuleEvent) result, Is.is(exceptionListenerResult));
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat( TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionBeginOrJoinAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionBeginOrJoinAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionJoinIfPossibleAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionJoinIfPossibleAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), Is.is(mockTransaction));
    }

    @Test(expected = AlreadyHandledMessagingException.class)
    public void testAlreadyHandledExceptionIsCatch() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_INDIFFERENT);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        transactionTemplate.execute(getFailureTransactionCallback(mockAlreadyHandledMessagingException));
    }

    protected TransactionTemplate createTransactionalTemplate(MuleTransactionConfig config)
    {
        return createNestedTransactionTemplate(config, mockMuleContext);
    }

    private MuleEvent configureExceptionListenerCall()
    {
        when(mockMessagingException.getEvent()).thenReturn(mockEvent);
        MuleEvent mockResultEvent = mock(MuleEvent.class);
        when(mockEvent.getFlowConstruct().getExceptionListener().handleException(mockMessagingException, mockEvent)).thenReturn(mockResultEvent);
        return mockResultEvent;
    }

}
