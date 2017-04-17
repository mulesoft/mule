/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ON_ERROR_CONTINUE;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(ERROR_HANDLING)
@Stories(ON_ERROR_CONTINUE)
@RunWith(MockitoJUnitRunner.class)
public class OnErrorContinueHandlerTestCase extends AbstractMuleContextTestCase {

  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
  protected MuleContext muleContext = mockContextWithServices();

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private MessagingException mockException;

  private Event muleEvent;

  private Message muleMessage = of("");

  @Mock
  private StreamCloserService mockStreamCloserService;
  @Spy
  private TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
  @Spy
  private TestTransaction mockXaTransaction = new TestTransaction(mockMuleContext, true);

  private Flow flow;

  private EventContext context;

  private OnErrorContinueHandler onErrorContinueHandler;

  @Before
  public void before() throws Exception {
    Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
    if (currentTransaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
    }

    flow = getTestFlow(muleContext);
    flow.initialise();
    onErrorContinueHandler = new OnErrorContinueHandler();
    onErrorContinueHandler.setMuleContext(mockMuleContext);
    onErrorContinueHandler.setFlowConstruct(flow);
    when(mockMuleContext.getStreamCloserService()).thenReturn(mockStreamCloserService);

    context = DefaultEventContext.create(flow, TEST_CONNECTOR);
    muleEvent = Event.builder(context).message(muleMessage).flow(flow).build();
  }

  @Test
  public void testHandleExceptionWithNoConfig() throws Exception {
    configureXaTransactionAndSingleResourceTransaction();

    Event resultEvent = onErrorContinueHandler.handleException(mockException, muleEvent);
    assertThat(resultEvent.getMessage().getPayload().getValue(), equalTo(muleEvent.getMessage().getPayload().getValue()));

    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
    verify(mockStreamCloserService).closeStream(any(Object.class));
  }

  @Test
  public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception {
    onErrorContinueHandler
        .setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
    onErrorContinueHandler.initialise();
    final Event result = onErrorContinueHandler.handleException(mockException, muleEvent);

    assertThat(result.getMessage().getPayload().getValue(), is("B"));
    assertThat(result.getError().isPresent(), is(false));
  }

  @Test
  public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception {
    Event lastEventCreated = Event.builder(context).message(muleMessage).flow(flow).build();
    onErrorContinueHandler
        .setMessageProcessors(asList(createChagingEventMessageProcessor(Event.builder(context).message(muleMessage).flow(flow)
            .build()),
                                     createChagingEventMessageProcessor(lastEventCreated)));
    onErrorContinueHandler.initialise();
    Event exceptionHandlingResult = onErrorContinueHandler.handleException(mockException, muleEvent);
    assertThat(exceptionHandlingResult.getCorrelationId(), is(lastEventCreated.getCorrelationId()));
  }

  /**
   * On fatal error, the exception strategies are not supposed to use Message.toString() as it could potentially log sensible
   * data.
   */
  @Test
  public void testMessageToStringNotCalledOnFailure() throws Exception {
    muleEvent = Event.builder(muleEvent).message(spy(muleMessage)).build();
    muleEvent = spy(muleEvent);
    when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);

    Event lastEventCreated = Event.builder(context).message(muleMessage).flow(flow).build();
    onErrorContinueHandler
        .setMessageProcessors(asList(createFailingEventMessageProcessor(Event.builder(context).message(muleMessage).flow(flow)
            .build()),
                                     createFailingEventMessageProcessor(lastEventCreated)));
    onErrorContinueHandler.initialise();
    when(muleEvent.getMessage().toString()).thenThrow(new RuntimeException("Message.toString() should not be called"));

    expectedException.expect(Exception.class);
    Event exceptionHandlingResult =
        exceptionHandlingResult = onErrorContinueHandler.handleException(mockException, muleEvent);
  }

  private Processor createChagingEventMessageProcessor(final Event lastEventCreated) {
    return event -> lastEventCreated;
  }

  private Processor createFailingEventMessageProcessor(final Event lastEventCreated) {
    return event -> {
      throw new DefaultMuleException(mockException);
    };
  }

  private Processor createSetStringMessageProcessor(final String appendText) {
    return event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(appendText).build()).build();
    };
  }

  private void configureXaTransactionAndSingleResourceTransaction() throws TransactionException {
    TransactionCoordination.getInstance().bindTransaction(mockXaTransaction);
    TransactionCoordination.getInstance().suspendCurrentTransaction();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
  }
}
