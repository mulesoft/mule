/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.exception.CatchMessagingExceptionStrategy;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionTemplateTestUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class TransactionalErrorHandlingExecutionTemplateTestCase extends TransactionalExecutionTemplateTestCase
{

    @Test
    public void testActionNoneAndXaTxAndFailureInCallback() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        MuleEvent mockExceptionListenerResultEvent = configureExceptionListenerCall();
        try
        {
            executionTemplate.execute(getFailureTransactionCallback());
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
            verify(mockMessagingException).setProcessedEvent(mockExceptionListenerResultEvent);
        }

        verify(mockTransaction).suspend();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        verify(mockTransaction).resume();
    }

    @Test
    public void testActionAlwaysBeginAndSuspendXaTxAndFailureCallback() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        mockTransaction.setXA(true);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(config);
        config.setFactory(new TestTransactionFactory(mockNewTransaction));
        MuleEvent exceptionListenerResult = configureExceptionListenerCall();
        try
        {
            executionTemplate.execute(getFailureTransactionCallback());
        }
        catch (MessagingException e)
        {
            assertThat(e, is(mockMessagingException));
            assertThat(e.getEvent(), is(exceptionListenerResult));
        }
        verify(mockTransaction).suspend();
        verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        verify(mockNewTransaction).rollback();
        verify(mockTransaction).resume();
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
        ExecutionTemplate transactionTemplate = createExecutionTemplate(config);
        MuleEvent exceptionListenerResult = configureExceptionListenerCall();
        try
        {
            transactionTemplate.execute(getFailureTransactionCallback());
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
            org.junit.Assert.assertThat(e.getEvent(), Is.is(exceptionListenerResult));
        }
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(1)).rollback();
        assertThat( TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }
    
    @Test
    public void testInnerTransactionCreatedAndResolved() throws Exception
    {
        ExecutionTemplate transactionTemplate = createExecutionTemplate(new MuleTransactionConfig());
        configureExceptionListenerCall();
        when(mockMessagingException.causedRollback()).thenReturn(false);
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockTransaction));
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
        }
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(1)).rollback();
        assertThat( TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testInnerTransactionCreatedAndNotResolved() throws Exception
    {
        ExecutionTemplate transactionTemplate = TransactionalErrorHandlingExecutionTemplate.createScopeExecutionTemplate(mockMuleContext, new MuleTransactionConfig(), mockMessagingExceptionHandler);
        configureCatchExceptionListenerCall();
        when(mockMessagingException.causedRollback()).thenReturn(false);
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockTransaction));
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
        }
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat( TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>notNullValue());
    }

    @Override
    protected ExecutionTemplate createExecutionTemplate(MuleTransactionConfig config)
    {
        return TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(mockMuleContext, config, mockMessagingExceptionHandler);
    }

    private MuleEvent configureExceptionListenerCall()
    {
        final MuleEvent mockResultEvent = mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());
        when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockResultEvent);
        when(mockMessagingExceptionHandler.handleException(mockMessagingException, mockEvent)).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                new DefaultMessagingExceptionStrategy().handleException((Exception)invocationOnMock.getArguments()[0],(MuleEvent)invocationOnMock.getArguments()[1]);
                return mockResultEvent;
            }
        });
        return mockResultEvent;
    }

    private MuleEvent configureCatchExceptionListenerCall()
    {
        final MuleEvent mockResultEvent = mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());
        when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockResultEvent);
        when(mockMessagingExceptionHandler.handleException(mockMessagingException, mockEvent)).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                new CatchMessagingExceptionStrategy().handleException((Exception)invocationOnMock.getArguments()[0],(MuleEvent)invocationOnMock.getArguments()[1]);
                return mockResultEvent;
            }
        });
        return mockResultEvent;
    }

    protected ExecutionCallback<MuleEvent> getFailureTransactionCallback() throws Exception
    {
        return TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException);
    }

}
