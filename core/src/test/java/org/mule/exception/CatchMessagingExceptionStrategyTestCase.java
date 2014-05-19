/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.util.StreamCloserService;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.transaction.TransactionCoordination;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CatchMessagingExceptionStrategyTestCase
{

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    @Mock
    private Exception mockException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleMessage mockMuleMessage;
    @Mock
    private StreamCloserService mockStreamCloserService;
    @Spy
    private TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
    @Spy
    private TestTransaction mockXaTransaction = new TestTransaction(mockMuleContext, true);


    private CatchMessagingExceptionStrategy catchMessagingExceptionStrategy;

    @Before
    public void before() throws Exception
    {
        Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
        if (currentTransaction != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
        }
        catchMessagingExceptionStrategy = new CatchMessagingExceptionStrategy();
        catchMessagingExceptionStrategy.setMuleContext(mockMuleContext);
        when(mockMuleContext.getStreamCloserService()).thenReturn(mockStreamCloserService);
        when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
    }

    @Test
    public void testHandleExceptionWithNoConfig() throws Exception
    {
        configureXaTransactionAndSingleResourceTransaction();

        MuleEvent resultEvent = catchMessagingExceptionStrategy.handleException(mockException, mockMuleEvent);
        assertThat(resultEvent, is(resultEvent));

        verify(mockMuleMessage, VerificationModeFactory.times(2)).setExceptionPayload(Matchers.<ExceptionPayload>any(ExceptionPayload.class));
        verify(mockTransaction, VerificationModeFactory.times(0)).setRollbackOnly();
        verify(mockTransaction, VerificationModeFactory.times(0)).commit();
        verify(mockTransaction, VerificationModeFactory.times(0)).rollback();
        verify(mockStreamCloserService).closeStream(Matchers.<Object>any(Object.class));
    }

    @Test
    public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception
    {
        catchMessagingExceptionStrategy.setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
        catchMessagingExceptionStrategy.initialise();
        catchMessagingExceptionStrategy.handleException(mockException,mockMuleEvent);
        verify(mockMuleEvent.getMessage(), times(1)).setPayload("A");
        verify(mockMuleEvent.getMessage(), times(1)).setPayload("B");
    }

    @Test
    public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception
    {
        MuleEvent lastEventCreated = mock(MuleEvent.class,Answers.RETURNS_DEEP_STUBS.get());
        catchMessagingExceptionStrategy.setMessageProcessors(asList(createChagingEventMessageProcessor(mock(MuleEvent.class,Answers.RETURNS_DEEP_STUBS.get())), createChagingEventMessageProcessor(lastEventCreated)));
        catchMessagingExceptionStrategy.initialise();
        MuleEvent exceptionHandlingResult = catchMessagingExceptionStrategy.handleException(mockException, mockMuleEvent);
        assertThat(exceptionHandlingResult, Is.is(lastEventCreated));
    }

    /**
     *  On fatal error, the exception strategies are not supposed to use MuleMessage.toString() as it could
     * potentially log sensible data.
     */
    @Test
    public void testMessageToStringNotCalledOnFailure() throws Exception
    {
        MuleEvent lastEventCreated = mock(MuleEvent.class,Answers.RETURNS_DEEP_STUBS.get());
        catchMessagingExceptionStrategy.setMessageProcessors(asList(createFailingEventMessageProcessor(mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get())), createFailingEventMessageProcessor(lastEventCreated)));
        catchMessagingExceptionStrategy.initialise();

        when(mockMuleEvent.getMessage().toString()).thenThrow(new RuntimeException("MuleMessage.toString() should not be called"));

        MuleEvent exceptionHandlingResult = exceptionHandlingResult = catchMessagingExceptionStrategy.handleException(mockException, mockMuleEvent);
    }

    private MessageProcessor createChagingEventMessageProcessor(final MuleEvent lastEventCreated)
    {
        return new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return lastEventCreated;
            }
        };
    }

    private MessageProcessor createFailingEventMessageProcessor(final MuleEvent lastEventCreated)
    {
        return new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new DefaultMuleException(mockException);
            }
        };
    }

    private MessageProcessor createSetStringMessageProcessor(final String appendText)
    {
        return new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload(appendText);
                return event;
            }
        };
    }

    private void configureXaTransactionAndSingleResourceTransaction() throws TransactionException
    {
        TransactionCoordination.getInstance().bindTransaction(mockXaTransaction);
        TransactionCoordination.getInstance().suspendCurrentTransaction();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    }
}
