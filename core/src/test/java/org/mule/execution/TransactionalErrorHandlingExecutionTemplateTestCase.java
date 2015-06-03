/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;
import org.mule.context.notification.ServerNotificationManager;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class TransactionalErrorHandlingExecutionTemplateTestCase extends TransactionalExecutionTemplateTestCase
{

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private TransactionConfig mockTransactionConfig;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private TransactionFactory mockTransactionFactory;

    @Before
    public void setUp() throws TransactionException
    {
        when(mockTransactionConfig.getFactory()).thenReturn(mockTransactionFactory);
        when(mockTransactionFactory.beginTransaction(mockMuleContext)).thenAnswer(new Answer<Transaction>()
        {
            @Override
            public Transaction answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                TransactionCoordination.getInstance().bindTransaction(mockTransaction);
                return mockTransaction;
            }
        });
    }

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
        MuleTransactionConfig config = createAlwaysBeginXaTransaction();
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

    @Test
    public void transactionCommitFailsCallsExceptionHandler() throws Exception
    {
        when(mockTransactionConfig.getAction()).thenReturn(ACTION_ALWAYS_BEGIN);
        ExecutionTemplate executionTemplate = createExecutionTemplate(mockTransactionConfig);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                TransactionCoordination.getInstance().unbindTransaction(mockTransaction);
                throw new RuntimeException();
            }
        }).when(mockTransaction).commit();
        configureCatchExceptionListenerCall();
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getEmptyTransactionCallback(mockEvent));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e)
        {
            assertThat(e.getEvent(), is(mockEvent));
        }
        verify(mockTransaction).commit();
        verify(mockMessagingExceptionHandler).handleException(any(MessagingException.class), eq(mockEvent));
    }

    @Override
    protected ExecutionTemplate createExecutionTemplate(TransactionConfig config)
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
                DefaultMessagingExceptionStrategy defaultMessagingExceptionStrategy = new DefaultMessagingExceptionStrategy();
                when(mockMuleContext.getNotificationManager()).thenReturn(mock(ServerNotificationManager.class));
                when(mockMuleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
                defaultMessagingExceptionStrategy.setMuleContext(mockMuleContext);
                defaultMessagingExceptionStrategy.handleException((Exception) invocationOnMock.getArguments()[0], (MuleEvent) invocationOnMock.getArguments()[1]);
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
                CatchMessagingExceptionStrategy exceptionStrategy = new CatchMessagingExceptionStrategy();
                exceptionStrategy.setMuleContext(mockMuleContext);
                when(mockMuleContext.getNotificationManager()).thenReturn(mock(ServerNotificationManager.class));
                when(mockMuleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
                exceptionStrategy.handleException((Exception) invocationOnMock.getArguments()[0], (MuleEvent) invocationOnMock.getArguments()[1]);
                return mockResultEvent;
            }
        });
        return mockResultEvent;
    }

    protected ExecutionCallback<MuleEvent> getFailureTransactionCallback() throws Exception
    {
        return TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException);
    }

    private MuleTransactionConfig createAlwaysBeginXaTransaction() throws TransactionException
    {
        return createTransactionConfig(true);
    }

    private MuleTransactionConfig createTransactionConfig(boolean isXa) throws TransactionException
    {
        mockTransaction.setXA(isXa);
        return new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    }

}
