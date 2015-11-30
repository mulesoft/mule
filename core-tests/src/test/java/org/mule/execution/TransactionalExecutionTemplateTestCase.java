/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplateTestUtils;

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

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class TransactionalExecutionTemplateTestCase extends AbstractMuleTestCase
{
    protected MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    @Mock
    protected MuleEvent RETURN_VALUE;
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
    protected MessagingExceptionHandler mockMessagingExceptionHandler;

    @Before
    public void unbindTransaction() throws Exception
    {
        Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
        if (currentTransaction != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
        }
        when(mockMessagingException.getStackTrace()).thenReturn(new StackTraceElement[0]);
    }

    @Test
    public void testActionIndifferentConfig() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_INDIFFERENT);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionNeverAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
    }

    @Test(expected = IllegalTransactionStateException.class)
    public void testActionNeverAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        executionTemplate.execute(getEmptyTransactionCallback());
    }

    @Test
    public void testActionNoneAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
    }

    @Test
    public void testActionNoneAndTxForCommit() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
    }


    @Test
    public void testActionNoneAndTxForRollback() throws Exception
    {
        when(mockTransaction.isRollbackOnly()).thenReturn(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
    }

    @Test
    public void testActionNoneAndXaTx() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction).suspend();
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
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
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
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
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionAlwaysBeginAndCommitTxAndCommitNewTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
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
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
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
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = executionTemplate.execute(getRollbackTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
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
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
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
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        Object result = executionTemplate.execute(getRollbackTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockNewTransaction).rollback();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction).suspend();
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }



    @Test(expected = IllegalTransactionStateException.class)
    public void testActionAlwaysJoinAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        executionTemplate.execute(getRollbackTransactionCallback());
    }

    @Test
    public void testActionAlwaysJoinAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getRollbackTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }


    @Test
    public void testActionBeginOrJoinAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionBeginOrJoinAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockTransaction));
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
    }

    @Test
    public void testActionJoinIfPossibleAndNoTx() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testActionJoinIfPossibleAndTx() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        Object result = executionTemplate.execute(getEmptyTransactionCallback());
        assertThat((MuleEvent) result, is(RETURN_VALUE));
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), Is.is(mockTransaction));
    }

    protected ExecutionTemplate createExecutionTemplate(TransactionConfig config)
    {
        return TransactionalExecutionTemplate.createTransactionalExecutionTemplate(mockMuleContext, config);
    }

    protected ExecutionCallback getEmptyTransactionCallback()
    {
        return TransactionTemplateTestUtils.getEmptyTransactionCallback(RETURN_VALUE);
    }

    protected ExecutionCallback getRollbackTransactionCallback()
    {
        return TransactionTemplateTestUtils.getRollbackTransactionCallback(RETURN_VALUE);
    }

}
