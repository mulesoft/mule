/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ON_ERROR_PROPAGATE;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

//TODO: MULE-9307 re-write junits for rollback exception strategy


@Feature(ERROR_HANDLING)
@Story(ON_ERROR_PROPAGATE)
public class OnErrorPropagateHandlerTestCase extends AbstractErrorHandlerTestCase {

  protected MuleContext muleContext = mockContextWithServices();
  private static final String DEFAULT_LOG_MESSAGE = "LOG";

  @Rule
  public ExpectedException expectedException = none();

  private final TestTransaction mockTransaction = spy(new TestTransaction(muleContext));
  private final TestTransaction mockXaTransaction = spy(new TestTransaction(muleContext, true));


  private OnErrorPropagateHandler onErrorPropagateHandler;

  public OnErrorPropagateHandlerTestCase(VerboseExceptions verbose) {
    super(verbose);
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
    if (currentTransaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
    }

    onErrorPropagateHandler = new OnErrorPropagateHandler();
    onErrorPropagateHandler.setAnnotations(getFlowComponentLocationAnnotations(flow.getName()));
    onErrorPropagateHandler.setMuleContext(muleContext);
    onErrorPropagateHandler.setNotificationFirer(mock(NotificationDispatcher.class));
  }

  @Test
  public void testHandleExceptionWithNoConfig() throws Exception {
    configureXaTransactionAndSingleResourceTransaction();
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    when(mockTransaction.getComponentLocation()).thenReturn(ofNullable(onErrorPropagateHandler.getLocation()));

    expectedException.expectCause(sameInstance(mockException));

    try {
      onErrorPropagateHandler.handleException(mockException, muleEvent);
    } finally {
      verify(mockException, never()).setHandled(anyBoolean());
      verify(mockTransaction, times(1)).setRollbackOnly();
      verify(mockTransaction, times(0)).commit();
      verify(mockTransaction, times(1)).rollback();
    }
  }

  @Test
  public void testHandleExceptionWithoutRollback() throws Exception {
    configureXaTransactionAndSingleResourceTransaction();
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    ComponentLocation location = mock(ComponentLocation.class);
    when(location.getRootContainerName()).thenReturn("");
    when(mockTransaction.getComponentLocation()).thenReturn(ofNullable(location));

    expectedException.expectCause(sameInstance(mockException));

    try {
      onErrorPropagateHandler.handleException(mockException, muleEvent);
    } finally {
      verify(mockException, never()).setHandled(anyBoolean());
      verify(mockTransaction, times(0)).setRollbackOnly();
      verify(mockTransaction, times(0)).commit();
      verify(mockTransaction, times(0)).rollback();
    }
  }

  @Test
  public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception {
    onErrorPropagateHandler
        .setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);

    expectedException.expectCause(sameInstance(mockException));

    try {
      onErrorPropagateHandler.handleException(mockException, muleEvent);
    } finally {
      verify(mockException, never()).setHandled(anyBoolean());
    }

  }

  @Test
  public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception {
    CoreEvent lastEventCreated = InternalEvent.builder(context).message(of("")).build();
    onErrorPropagateHandler
        .setMessageProcessors(asList(createChagingEventMessageProcessor(InternalEvent.builder(context).message(of(""))
            .build()),
                                     createChagingEventMessageProcessor(lastEventCreated)));
    onErrorPropagateHandler.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);

    expectedException.expectCause(sameInstance(mockException));

    try {
      onErrorPropagateHandler.handleException(mockException, muleEvent);
    } finally {
      verify(mockException, never()).setHandled(anyBoolean());
    }

  }

  @Test
  public void testHandleExceptionWithErrorInHandling() throws Exception {
    final NullPointerException innerException = new NullPointerException();
    onErrorPropagateHandler.setMessageProcessors(asList(createFailingEventMessageProcessor(innerException)));
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);

    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);

    expectedException.expect(hasRootCause(sameInstance(innerException)));

    try {
      onErrorPropagateHandler.handleException(mockException, muleEvent);
    } finally {
      verify(mockException, never()).setHandled(anyBoolean());
    }

  }

  /**
   * On fatal error, the exception strategies are not supposed to use Message.toString() as it could potentially log sensible
   * data.
   */
  @Test
  public void testMessageToStringNotCalledOnFailure() throws Exception {
    muleEvent = CoreEvent.builder(muleEvent).message(spy(of(""))).build();
    muleEvent = spy(muleEvent);
    when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);
    when(mockException.getEvent()).thenReturn(muleEvent);

    onErrorPropagateHandler
        .setMessageProcessors(asList(createFailingEventMessageProcessor(mockException),
                                     createFailingEventMessageProcessor(mockException)));
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    when(muleEvent.getMessage().toString()).thenThrow(new RuntimeException("Message.toString() should not be called"));

    expectedException.expect(Exception.class);
    onErrorPropagateHandler.handleException(mockException, muleEvent);
  }

  private Processor createChagingEventMessageProcessor(final CoreEvent lastEventCreated) {
    return event -> lastEventCreated;
  }

  private Processor createFailingEventMessageProcessor(Exception toThrow) {
    return event -> {
      throw new DefaultMuleException(toThrow);
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
