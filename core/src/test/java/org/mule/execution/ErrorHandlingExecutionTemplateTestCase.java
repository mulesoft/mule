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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.transaction.TransactionTemplateTestUtils.getEmptyTransactionCallback;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transaction.Transaction;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;
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
public class ErrorHandlingExecutionTemplateTestCase extends AbstractMuleTestCase
{
    private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    @Mock
    private MuleEvent RETURN_VALUE;
    @Mock
    private MessagingException mockMessagingException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockEvent;
    @Spy
    protected TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
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
    public void testSuccessfulExecution() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        Object result = executionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat((MuleEvent) result, is(RETURN_VALUE));
    }

    private ExecutionTemplate createExceptionHandlingTransactionTemplate()
    {
        return ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate(mockMuleContext, mockMessagingExceptionHandler);
    }

    @Test
    public void testFailureException() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        MuleEvent mockResultEvent = mock(MuleEvent.class);
        when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockEvent).thenReturn(mockResultEvent);
        when(mockMessagingExceptionHandler.handleException(mockMessagingException, mockEvent)).thenReturn(mockResultEvent);
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
            verify(mockMessagingException).setProcessedEvent(mockResultEvent);
        }
    }

    @Test
    public void testTransactionIsMarkedRollbackOnExceptionByDefault() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null,null);
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).rollback();
    }

    @Test
    public void testTransactionIsNotRollbackOnEveryException() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "*");
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
    }

    @Test
    public void testTransactionIsNotRollbackOnMatcherRegexPatternException() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "org.mule.ap*");
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
    }

    @Test
    public void testTransactionIsNotRollbackOnClassHierarchyPatternException() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "org.mule.api.MuleException+");
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
    }

    @Test
    public void testTransactionIsNotRollbackOnClassExactlyPatternException() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "org.mule.api.MessagingException");
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(new MessagingException(mockEvent, null)));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
    }

    @Test
    public void testTransactionIsRollbackOnPatternAppliesToRollbackAndCommit() throws Exception
    {
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener("org.mule.api.MuleException+", "org.mule.api.MessagingException");
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(1)).setRollbackOnly();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(1)).rollback();
    }

    @Test
    public void testSuspendedTransactionNotResumedOnException() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        TransactionCoordination.getInstance().suspendCurrentTransaction();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
        configureExceptionListener(null,null);
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(0)).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    @Test
    public void testSuspendedTransactionNotResumedAndNewTransactionResolvedOnException() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        TransactionCoordination.getInstance().suspendCurrentTransaction();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
        configureExceptionListener(null,null);
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        final Transaction mockNewTransaction = spy(new TestTransaction(mockMuleContext));
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockNewTransaction));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(0)).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        verify(mockNewTransaction, VerificationModeFactory.times(1)).rollback();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockNewTransaction, VerificationModeFactory.times(1)).setRollbackOnly();
        assertThat(TransactionCoordination.getInstance().getTransaction(),IsNull.<Object>nullValue());
    }

    @Test
    public void testTransactionIsResolved() throws Exception
    {
        configureExceptionListener(null,null);
        ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
        try
        {
            executionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockTransaction));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction, VerificationModeFactory.times(1)).setRollbackOnly();
        verify(mockTransaction, VerificationModeFactory.times(1)).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }

    private void configureExceptionListener(final String rollbackFilter,final String commitFilter)
    {
        when(mockMessagingException.getEvent()).thenReturn(mockEvent);
        when(mockMuleContext.getNotificationManager()).thenReturn(mock(ServerNotificationManager.class));
        when(mockMuleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
        when(mockMessagingExceptionHandler.handleException(any(MessagingException.class), any(MuleEvent.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                DefaultMessagingExceptionStrategy defaultMessagingExceptionStrategy = new DefaultMessagingExceptionStrategy();
                defaultMessagingExceptionStrategy.setMuleContext(mockMuleContext);
                if (rollbackFilter != null)
                {
                    defaultMessagingExceptionStrategy.setRollbackTxFilter(new WildcardFilter(rollbackFilter));
                }
                if (commitFilter != null)
                {
                    defaultMessagingExceptionStrategy.setCommitTxFilter(new WildcardFilter(commitFilter));
                }
                defaultMessagingExceptionStrategy.handleException((Exception) invocationOnMock.getArguments()[0], (MuleEvent) invocationOnMock.getArguments()[1]);
                return null;
            }
        });
    }


}
