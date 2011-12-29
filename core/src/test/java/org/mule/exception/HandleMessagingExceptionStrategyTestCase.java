/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.util.StreamCloserService;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.transaction.TransactionCoordination;

import static org.hamcrest.core.Is.is;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HandleMessagingExceptionStrategyTestCase
{

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    @Mock
    private Exception mockException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;
    @Mock
    private MuleMessage mockMuleMessage;
    @Mock
    private StreamCloserService mockStreamCloserService;
    @Spy
    private TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
    @Spy
    private TestTransaction mockXaTransaction = new TestTransaction(mockMuleContext, true);


    private HandleMessagingExceptionStrategy handleMessagingExceptionStrategy;

    @Before
    public void before() throws Exception
    {
        Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
        if (currentTransaction != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
        }
        handleMessagingExceptionStrategy = new HandleMessagingExceptionStrategy(mockMuleContext);
        when(mockMuleContext.getRegistry().lookupObject(
                MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE)).thenReturn(mockStreamCloserService);
        when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
    }

    @Test
    public void testHandleExceptionWithNoConfig() throws Exception
    {
        configureXaTransactionAndSingleResourceTransaction();

        MuleEvent resultEvent = handleMessagingExceptionStrategy.handleException(mockException, mockMuleEvent, null);
        assertThat(resultEvent, is(resultEvent));

        verify(mockMuleMessage).setExceptionPayload(Matchers.<ExceptionPayload>any(ExceptionPayload.class));
        verify(mockMuleMessage).setExceptionPayload(null);
        verify(mockTransaction).commit();
        verify(mockXaTransaction).resume();
        verify(mockStreamCloserService).closeStream(Matchers.<Object>any(Object.class));
    }

    @Test
    public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception
    {
        handleMessagingExceptionStrategy.setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
        handleMessagingExceptionStrategy.initialise();
        handleMessagingExceptionStrategy.handleException(mockException,mockMuleEvent);
        verify(mockMuleEvent.getMessage(), times(1)).setPayload("A");
        verify(mockMuleEvent.getMessage(), times(1)).setPayload("B");
    }

    @Test
    public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception
    {
        MuleEvent lastEventCreated = mock(MuleEvent.class,Answers.RETURNS_DEEP_STUBS.get());
        handleMessagingExceptionStrategy.setMessageProcessors(asList(createChagingEventMessageProcessor(mock(MuleEvent.class)), createChagingEventMessageProcessor(lastEventCreated)));
        handleMessagingExceptionStrategy.initialise();
        MuleEvent exceptionHandlingResult = handleMessagingExceptionStrategy.handleException(mockException, mockMuleEvent);
        assertThat(exceptionHandlingResult, Is.is(lastEventCreated));
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
