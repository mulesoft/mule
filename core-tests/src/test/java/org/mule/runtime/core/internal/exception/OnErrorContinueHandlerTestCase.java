/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_MULE_UNKNOWN_ERROR_RAISED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.getInstance;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ON_ERROR_CONTINUE;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SUPPORTABILITY;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SupportabilityStory.ALERTS;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.exception.AbstractDeclaredExceptionListener;
import org.mule.runtime.core.privileged.exception.DefaultExceptionListener;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.tck.processor.ContextPropagationChecker;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import reactor.core.publisher.Flux;

@Feature(ERROR_HANDLING)
@Story(ON_ERROR_CONTINUE)
public class OnErrorContinueHandlerTestCase extends AbstractErrorHandlerTestCase {

  protected MuleContext muleContext = mockContextWithServices();
  private static final String DEFAULT_LOG_MESSAGE = "LOG";

  private TestTransaction mockTransaction;
  private TestTransaction mockXaTransaction;

  private OnErrorContinueHandler onErrorContinueHandler;

  public OnErrorContinueHandlerTestCase(VerboseExceptions verbose) {
    super(verbose);
  }

  @Override
  protected AbstractDeclaredExceptionListener getErrorHandler() {
    return onErrorContinueHandler;
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    Transaction currentTransaction = getInstance().getTransaction();
    if (currentTransaction != null) {
      getInstance().unbindTransaction(currentTransaction);
    }

    onErrorContinueHandler = new OnErrorContinueHandler();
    onErrorContinueHandler.setAnnotations(getFlowComponentLocationAnnotations(flow.getName()));
    onErrorContinueHandler.setMuleContext(muleContext);
    onErrorContinueHandler.setExceptionListener(new DefaultExceptionListener());

    NotificationDispatcher notificationDispatcher = getNotificationDispatcher(muleContext);
    mockTransaction =
        spy(new TestTransaction("appName", notificationDispatcher));
    mockXaTransaction =
        spy(new TestTransaction("appName", notificationDispatcher, true));
  }

  @Test
  public void handleExceptionWithNoConfig() throws Exception {
    when(mockException.handled()).thenReturn(true);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    configureXaTransactionAndSingleResourceTransaction();

    initialiseIfNeeded(onErrorContinueHandler, muleContext);
    startIfNeeded(onErrorContinueHandler);

    CoreEvent resultEvent = onErrorContinueHandler.handleException(mockException, muleEvent);
    assertThat(resultEvent.getMessage().getPayload().getValue(), equalTo(muleEvent.getMessage().getPayload().getValue()));

    verify(mockException).setHandled(true);
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
  }

  @Test
  public void handleExceptionWithConfiguredMessageProcessors() throws Exception {
    onErrorContinueHandler
        .setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
    initialiseIfNeeded(onErrorContinueHandler, muleContext);
    startIfNeeded(onErrorContinueHandler);

    when(mockException.handled()).thenReturn(true);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    final CoreEvent result = onErrorContinueHandler.handleException(mockException, muleEvent);

    verify(mockException).setHandled(true);
    assertThat(result.getMessage().getPayload().getValue(), is("B"));
    assertThat(result.getError().isPresent(), is(false));
  }

  @Test
  public void handleExceptionWithMessageProcessorsChangingEvent() throws Exception {
    CoreEvent lastEventCreated = CoreEvent.builder(context).message(of("")).build();
    onErrorContinueHandler
        .setMessageProcessors(asList(createChagingEventMessageProcessor(CoreEvent.builder(context).message(of(""))
            .build()),
                                     createChagingEventMessageProcessor(lastEventCreated)));
    onErrorContinueHandler.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(onErrorContinueHandler, muleContext);
    startIfNeeded(onErrorContinueHandler);

    when(mockException.handled()).thenReturn(true);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    CoreEvent exceptionHandlingResult = onErrorContinueHandler.handleException(mockException, muleEvent);

    verify(mockException).setHandled(true);
    assertThat(exceptionHandlingResult.getCorrelationId(), is(lastEventCreated.getCorrelationId()));
  }

  /**
   * On fatal error, the exception strategies are not supposed to use Message.toString() as it could potentially log sensible
   * data.
   */
  @Test
  public void messageToStringNotCalledOnFailure() throws Exception {
    muleEvent = builder(muleEvent).message(spy(of(""))).build();
    muleEvent = spy(muleEvent);

    when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    onErrorContinueHandler.setMessageProcessors(asList(createFailingEventMessageProcessor(),
                                                       createFailingEventMessageProcessor()));
    initialiseIfNeeded(onErrorContinueHandler, muleContext);
    when(muleEvent.getMessage().toString()).thenThrow(new RuntimeException("Message.toString() should not be called"));

    assertThrows(Exception.class, () -> onErrorContinueHandler.handleException(mockException, muleEvent));
  }

  @Test
  public void subscriberContextPropagation() throws MuleException {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    onErrorContinueHandler
        .setMessageProcessors(singletonList(contextPropagationChecker));

    initialiseIfNeeded(onErrorContinueHandler, muleContext);

    AtomicReference<CoreEvent> resultRef = new AtomicReference<>();
    final Consumer<Exception> router = onErrorContinueHandler
        .router(pub -> Flux.from(pub)
            .contextWrite(contextPropagationChecker.contextPropagationFlag()),
                e -> resultRef.set(e),
                t -> {
                  throw new MuleRuntimeException(t);
                });

    when(mockException.getEvent()).thenReturn(muleEvent);
    when(mockException.handled()).thenReturn(true);
    addErrorType(CONNECTIVITY);
    router.accept(mockException);

    assertThat(resultRef.get(), not(nullValue()));
  }

  @Test
  @Feature(SUPPORTABILITY)
  @Story(ALERTS)
  public void unknownErrorAlertTriggered() throws Exception {
    addErrorType(UNKNOWN);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    initialiseIfNeeded(onErrorContinueHandler, muleContext);
    onErrorContinueHandler.setAlertingSupport(alertingSupport);
    startIfNeeded(onErrorContinueHandler);

    onErrorContinueHandler.handleException(mockException, muleEvent);

    verify(alertingSupport).triggerAlert(ALERT_MULE_UNKNOWN_ERROR_RAISED,
                                         mockException.toString());
  }

  @Test
  @Feature(SUPPORTABILITY)
  @Story(ALERTS)
  public void knownErrorAlertNotTriggered() throws Exception {
    addErrorType(CONNECTIVITY);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    initialiseIfNeeded(onErrorContinueHandler, muleContext);
    onErrorContinueHandler.setAlertingSupport(alertingSupport);
    startIfNeeded(onErrorContinueHandler);

    onErrorContinueHandler.handleException(mockException, muleEvent);

    verify(alertingSupport, never()).triggerAlert(any());
    verify(alertingSupport, never()).triggerAlert(any(), any());
  }

  private Processor createChagingEventMessageProcessor(final CoreEvent lastEventCreated) {
    return event -> builder(event).message(lastEventCreated.getMessage()).build();
  }

  private Processor createFailingEventMessageProcessor() {
    return event -> {
      throw new DefaultMuleException(mockException);
    };
  }

  private Processor createSetStringMessageProcessor(final String appendText) {
    return event -> builder(event).message(InternalMessage.builder(event.getMessage()).value(appendText).build()).build();
  }

  private void configureXaTransactionAndSingleResourceTransaction() throws TransactionException {
    getInstance().bindTransaction(mockXaTransaction);
    getInstance().suspendCurrentTransaction();
    getInstance().bindTransaction(mockTransaction);
  }
}
