/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ON_ERROR_CONTINUE;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
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

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ON_ERROR_CONTINUE)
@RunWith(MockitoJUnitRunner.class)
public class OnErrorContinueHandlerTestCase extends AbstractMuleContextTestCase {

  protected MuleContext muleContext = mockContextWithServices();
  private static final String DEFAULT_LOG_MESSAGE = "LOG";

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private MessagingException mockException;

  private CoreEvent muleEvent;

  private Message muleMessage = of("");

  @Spy
  private TestTransaction mockTransaction = new TestTransaction(muleContext);
  @Spy
  private TestTransaction mockXaTransaction = new TestTransaction(muleContext, true);

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
    onErrorContinueHandler.setAnnotations(getFlowComponentLocationAnnotations(flow.getName()));
    onErrorContinueHandler.setMuleContext(muleContext);
    onErrorContinueHandler.setNotificationFirer(mock(NotificationDispatcher.class));

    context = create(flow, TEST_CONNECTOR_LOCATION);
    muleEvent = InternalEvent.builder(context).message(muleMessage).build();
  }

  @Test
  public void testHandleExceptionWithNoConfig() throws Exception {
    configureXaTransactionAndSingleResourceTransaction();
    when(mockException.handled()).thenReturn(true);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);

    CoreEvent resultEvent = onErrorContinueHandler.handleException(mockException, muleEvent);
    assertThat(resultEvent.getMessage().getPayload().getValue(), equalTo(muleEvent.getMessage().getPayload().getValue()));

    verify(mockException).setHandled(true);
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
  }

  @Test
  public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception {
    onErrorContinueHandler
        .setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
    onErrorContinueHandler.setMuleContext(muleContext);
    onErrorContinueHandler.initialise();
    when(mockException.handled()).thenReturn(true);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    final CoreEvent result = onErrorContinueHandler.handleException(mockException, muleEvent);

    verify(mockException).setHandled(true);
    assertThat(result.getMessage().getPayload().getValue(), is("B"));
    assertThat(result.getError().isPresent(), is(false));
  }

  @Test
  public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception {
    CoreEvent lastEventCreated = InternalEvent.builder(context).message(muleMessage).build();
    onErrorContinueHandler
        .setMessageProcessors(asList(createChagingEventMessageProcessor(InternalEvent.builder(context).message(muleMessage)
            .build()),
                                     createChagingEventMessageProcessor(lastEventCreated)));
    onErrorContinueHandler.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(onErrorContinueHandler, true, muleContext);
    when(mockException.handled()).thenReturn(true);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    CoreEvent exceptionHandlingResult = onErrorContinueHandler.handleException(mockException, muleEvent);

    verify(mockException).setHandled(true);
    assertThat(exceptionHandlingResult.getCorrelationId(), is(lastEventCreated.getCorrelationId()));
  }

  /**
   * On fatal error, the exception strategies are not supposed to use Message.toString() as it could potentially log sensible
   * data.
   */
  @Test
  public void testMessageToStringNotCalledOnFailure() throws Exception {
    muleEvent = CoreEvent.builder(muleEvent).message(spy(muleMessage)).build();
    muleEvent = spy(muleEvent);
    when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);

    CoreEvent lastEventCreated = InternalEvent.builder(context).message(muleMessage).build();
    onErrorContinueHandler
        .setMessageProcessors(asList(createFailingEventMessageProcessor(InternalEvent.builder(context).message(muleMessage)
            .build()),
                                     createFailingEventMessageProcessor(lastEventCreated)));
    onErrorContinueHandler.initialise();
    when(muleEvent.getMessage().toString()).thenThrow(new RuntimeException("Message.toString() should not be called"));

    expectedException.expect(Exception.class);
    onErrorContinueHandler.handleException(mockException, muleEvent);
  }

  private Processor createChagingEventMessageProcessor(final CoreEvent lastEventCreated) {
    return event -> lastEventCreated;
  }

  private Processor createFailingEventMessageProcessor(final CoreEvent lastEventCreated) {
    return event -> {
      throw new DefaultMuleException(mockException);
    };
  }

  private Processor createSetStringMessageProcessor(final String appendText) {
    return event -> {
      return CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).value(appendText).build()).build();
    };
  }

  private void configureXaTransactionAndSingleResourceTransaction() throws TransactionException {
    TransactionCoordination.getInstance().bindTransaction(mockXaTransaction);
    TransactionCoordination.getInstance().suspendCurrentTransaction();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
  }
}
