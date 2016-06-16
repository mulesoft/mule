/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CatchMessagingExceptionStrategyTestCase extends AbstractMuleContextTestCase
{

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    @Mock
    private Exception mockException;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MutableMuleMessage mockMuleMessage;
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
        assertThat(resultEvent, is(mockMuleEvent));

        verify(mockTransaction, times(0)).setRollbackOnly();
        verify(mockTransaction, times(0)).commit();
        verify(mockTransaction, times(0)).rollback();
        verify(mockStreamCloserService).closeStream(Matchers.any(Object.class));
    }

    @Mock(answer = RETURNS_DEEP_STUBS)
    protected MuleContext muleContext;

    @Test
    public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception
    {
        MuleMessage message = spy(new DefaultMuleMessage("", muleContext));
        when(mockMuleEvent.getMessage()).thenReturn(message);

        catchMessagingExceptionStrategy.setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
        catchMessagingExceptionStrategy.initialise();
        catchMessagingExceptionStrategy.handleException(mockException,mockMuleEvent);
        verify(mockMuleEvent, times(1)).setMessage(argThat(new ArgumentMatcher<MuleMessage>()
        {
            @Override
            public boolean matches(Object argument)
            {
                return ((MuleMessage) argument).getPayload().equals("A");
            }
        }));
        verify(mockMuleEvent, times(1)).setMessage(argThat(new ArgumentMatcher<MuleMessage>()
        {
            @Override
            public boolean matches(Object argument)
            {
                return ((MuleMessage) argument).getPayload().equals("B");
            }
        }));
    }

    @Test
    public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception
    {
        MuleEvent lastEventCreated = mock(MuleEvent.class,Answers.RETURNS_DEEP_STUBS.get());
        catchMessagingExceptionStrategy.setMessageProcessors(asList(createChagingEventMessageProcessor(mock(MuleEvent.class,Answers.RETURNS_DEEP_STUBS.get())), createChagingEventMessageProcessor(lastEventCreated)));
        catchMessagingExceptionStrategy.initialise();
        MuleEvent exceptionHandlingResult = catchMessagingExceptionStrategy.handleException(mockException, mockMuleEvent);
        assertThat(exceptionHandlingResult.getId(), is(lastEventCreated.getId()));
        assertThat(exceptionHandlingResult.getMessage().getUniqueId(), is(lastEventCreated.getMessage().getUniqueId()));
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

    private MuleEvent createNonBlockingTestEvent() throws Exception
    {
        Flow flow = MuleTestUtils.getTestFlow(muleContext);
        flow.setProcessingStrategy(new NonBlockingProcessingStrategy());
        return new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, muleContext),
                                    MessageExchangePattern.REQUEST_RESPONSE,
                                    new SensingNullReplyToHandler(), flow);
    }

    private MessageProcessor createChagingEventMessageProcessor(final MuleEvent lastEventCreated)
    {
        return event -> lastEventCreated;
    }

    private MessageProcessor createFailingEventMessageProcessor(final MuleEvent lastEventCreated)
    {
        return event ->
        {
            throw new DefaultMuleException(mockException);
        };
    }

    private MessageProcessor createSetStringMessageProcessor(final String appendText)
    {
        return event ->
        {
            event.setMessage(event.getMessage().transform(msg -> {
                msg.setPayload(appendText);
                return msg;
            }));
            return event;
        };
    }

    private void configureXaTransactionAndSingleResourceTransaction() throws TransactionException
    {
        TransactionCoordination.getInstance().bindTransaction(mockXaTransaction);
        TransactionCoordination.getInstance().suspendCurrentTransaction();
        TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    }
}
