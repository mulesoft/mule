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
import org.mule.api.transaction.Transaction;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.testmodels.mule.TestTransaction;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mule.transaction.TransactionTemplateFactory.createExceptionHandlingTransactionTemplate;
import static org.mule.transaction.TransactionTemplateTestUtils.getEmptyTransactionCallback;

@RunWith(MockitoJUnitRunner.class)
public class ErrorHandlingTransactionTemplateTestCase
{
    private static final Object RETURN_VALUE = new Object();
    private MuleContext mockMuleContext = mock(MuleContext.class);
    @Mock
    private MessagingException mockMessagingException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockEvent;
    @Spy
    protected TestTransaction mockTransaction = new TestTransaction(mockMuleContext);



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
    public void testSuccessfulExecution() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        Object result = transactionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
        assertThat(result, is(RETURN_VALUE));
    }

    @Test
    public void testFailureException() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        MuleEvent mockResultEvent = mock(MuleEvent.class);
        when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockEvent).thenReturn(mockResultEvent);
        when(mockEvent.getFlowConstruct().getExceptionListener().handleException(mockMessagingException, mockEvent)).thenReturn(mockResultEvent);
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
            verify(mockMessagingException).setProcessedEvent(mockResultEvent);
        }
    }

    @Test
    public void testTransactionIsRollbackOnExceptionByDefault() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null,null);
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).rollback();
    }

    @Test
    public void testTransactionIsCommitOnEveryException() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "*");
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).commit();
    }

    @Test
    public void testTransactionIsCommitOnMatcherRegexPatternException() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "org.mule.ap*");
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).commit();
    }

    @Test
    public void testTransactionIsCommitOnClassHierarchyPatternException() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "org.mule.api.MuleException+");
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).commit();
    }

    @Test
    public void testTransactionIsCommitOnClassExactlyPatternException() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener(null, "org.mule.api.MessagingException");
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(new MessagingException(mockEvent,null)));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).commit();
    }

    @Test
    public void testTransactionIsRollbackOnPatternAppliesToRollbackAndCommit() throws Exception
    {
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        configureExceptionListener("org.mule.api.MuleException+", "org.mule.api.MessagingException");
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).rollback();
    }

    @Test
    public void testSuspendedTransactionResumedOnException() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        TransactionCoordination.getInstance().suspendCurrentTransaction();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
        configureExceptionListener(null,null);
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        org.junit.Assert.assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(),Is.is(mockTransaction));
    }

    @Test
    public void testSuspendedTransactionResumedAndNewTransactionResolvedOnException() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        TransactionCoordination.getInstance().suspendCurrentTransaction();
        assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
        configureExceptionListener(null,null);
        TransactionTemplate transactionTemplate = createExceptionHandlingTransactionTemplate(mockMuleContext);
        final Transaction mockNewTransaction = spy(new TestTransaction(mockMuleContext));
        try
        {
            ;
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallbackStartsTransaction(mockMessagingException,mockNewTransaction));
            fail("MessagingException must be thrown");
        }
        catch (MessagingException e) {}
        verify(mockTransaction).resume();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        verify(mockNewTransaction, VerificationModeFactory.times(1)).rollback();
        verify(mockNewTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockNewTransaction, VerificationModeFactory.times(1)).setRollbackOnly();
        org.junit.Assert.assertThat((TestTransaction)TransactionCoordination.getInstance().getTransaction(),Is.is(mockTransaction));
    }

    private void configureExceptionListener(final String rollbackFilter,final String commitFilter)
    {
        when(mockMessagingException.getEvent()).thenReturn(mockEvent);
        when(mockEvent.getFlowConstruct().getExceptionListener().handleException(any(MessagingException.class),any(MuleEvent.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                DefaultMessagingExceptionStrategy defaultMessagingExceptionStrategy = new DefaultMessagingExceptionStrategy();
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
