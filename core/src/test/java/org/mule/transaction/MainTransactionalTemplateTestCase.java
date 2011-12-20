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
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.transaction.TransactionTemplateFactory.createMainTransactionTemplate;
import static org.mule.transaction.TransactionTemplateTestUtils.getFailureTransactionCallback;

@RunWith(MockitoJUnitRunner.class)
public class MainTransactionalTemplateTestCase extends NestedTransactionTemplateTestCase
{

    @Test
    public void testActionNoneAndXaTxAndFailureInCallback() throws Exception
    {
        mockTransaction.setXA(true);
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        MuleEvent mockExceptionListenerResultEvent = configureExceptionListenerCall();
        try
        {
            transactionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
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
        //TODO fix this issue. resume should be called despite exception
        //Mockito.verify(mockTransaction).resume();
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
        try
        {
            transactionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
            org.junit.Assert.assertThat(e.getEvent(), Is.is(exceptionListenerResult));
        }
        verify(mockTransaction).suspend();
        verify(mockTransaction,VerificationModeFactory.times(0)).commit();
        verify(mockTransaction,VerificationModeFactory.times(0)).rollback();
        //TODO fix this issue. New transaction should be resolved - Xa transaction should be resumed - Xa transaction should be in TransactionCoordinator
        //Mockito.verify(mockNewTransaction).rollback();
        //Mockito.verify(mockTransaction).resume();
        //assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
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
        try
        {
            transactionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
            org.junit.Assert.assertThat(e.getEvent(), Is.is(exceptionListenerResult));
        }
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        assertThat( TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    }


    @Override
    protected TransactionTemplate createTransactionalTemplate(MuleTransactionConfig config)
    {
        return createMainTransactionTemplate(config, mockMuleContext);
    }

    private MuleEvent configureExceptionListenerCall()
    {
        MuleEvent mockResultEvent = mock(MuleEvent.class);
        when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockEvent).thenReturn(mockResultEvent);
        when(mockEvent.getFlowConstruct().getExceptionListener().handleException(mockMessagingException, mockEvent)).thenReturn(mockResultEvent);
        return mockResultEvent;
    }

}
