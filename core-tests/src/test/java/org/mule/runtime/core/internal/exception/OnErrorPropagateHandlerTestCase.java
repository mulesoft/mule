/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_MULE_UNKNOWN_ERROR_RAISED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ON_ERROR_PROPAGATE;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SUPPORTABILITY;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SupportabilityStory.ALERTS;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.just;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.exception.AbstractDeclaredExceptionListener;
import org.mule.runtime.core.privileged.exception.DefaultExceptionListener;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.tck.processor.ContextPropagationChecker;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

//TODO: MULE-9307 re-write junits for rollback exception strategy

@Feature(ERROR_HANDLING)
@Story(ON_ERROR_PROPAGATE)
public class OnErrorPropagateHandlerTestCase extends AbstractErrorHandlerTestCase {

  protected MuleContext muleContext = mockContextWithServices();
  private static final String DEFAULT_LOG_MESSAGE = "LOG";
  private static final Logger LOGGER = getLogger(OnErrorPropagateHandlerTestCase.class);

  private final NotificationDispatcher notificationDispatcher = getNotificationDispatcher(muleContext);
  private final TestTransaction mockTransaction =
      spy(new TestTransaction("appName", notificationDispatcher));
  private final TestTransaction mockXaTransaction =
      spy(new TestTransaction("appNAme", notificationDispatcher, true));

  private TestOnErrorPropagateHandler onErrorPropagateHandler;

  public OnErrorPropagateHandlerTestCase(VerboseExceptions verbose) throws RegistrationException {
    super(verbose);
  }

  @Override
  protected AbstractDeclaredExceptionListener getErrorHandler() {
    return onErrorPropagateHandler;
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
    if (currentTransaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
    }

    onErrorPropagateHandler = new TestOnErrorPropagateHandler();
    onErrorPropagateHandler.setAnnotations(getFlowComponentLocationAnnotations(flow.getName()));
    onErrorPropagateHandler.setMuleContext(muleContext);
    onErrorPropagateHandler.setExceptionListener(new DefaultExceptionListener());
  }

  @Test
  public void testHandleExceptionWithNoConfig() throws Exception {
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    when(mockTransaction.getComponentLocation()).thenReturn(ofNullable(onErrorPropagateHandler.getLocation()));
    addErrorType(CONNECTIVITY);

    configureXaTransactionAndSingleResourceTransaction();

    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    startIfNeeded(onErrorPropagateHandler);

    var thrown = assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));
    assertThat(thrown.getCause(), sameInstance(mockException));

    verify(mockException, never()).setHandled(anyBoolean());
    verify(mockTransaction, times(1)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(1)).rollback();
  }

  @Test
  public void testHandleExceptionWithoutRollback() throws Exception {
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    configureXaTransactionAndSingleResourceTransaction();
    ComponentLocation location = mock(ComponentLocation.class);
    when(location.getRootContainerName()).thenReturn("");
    when(location.getLocation()).thenReturn("");
    when(mockTransaction.getComponentLocation()).thenReturn(ofNullable(location));

    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    startIfNeeded(onErrorPropagateHandler);

    var thrown = assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));
    assertThat(thrown.getCause(), sameInstance(mockException));

    verify(mockException, never()).setHandled(anyBoolean());
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
  }

  @Test
  public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception {
    onErrorPropagateHandler
        .setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    startIfNeeded(onErrorPropagateHandler);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    var thrown = assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));
    assertThat(thrown.getCause(), sameInstance(mockException));

    verify(mockException, never()).setHandled(anyBoolean());
  }

  @Test
  public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception {
    CoreEvent lastEventCreated = CoreEvent.builder(context).message(of("")).build();
    onErrorPropagateHandler
        .setMessageProcessors(asList(createChangingEventMessageProcessor(CoreEvent.builder(context).message(of(""))
            .build()),
                                     createChangingEventMessageProcessor(lastEventCreated)));
    onErrorPropagateHandler.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    startIfNeeded(onErrorPropagateHandler);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    var thrown = assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));
    assertThat(thrown.getCause(), sameInstance(mockException));

    verify(mockException, never()).setHandled(anyBoolean());
  }

  @Test
  public void testHandleExceptionWithErrorInHandling() throws Exception {
    final NullPointerException innerException = new NullPointerException();
    onErrorPropagateHandler.setMessageProcessors(singletonList(createFailingEventMessageProcessor(innerException)));
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    startIfNeeded(onErrorPropagateHandler);

    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    var thrown = assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));
    assertThat(thrown, hasRootCause(sameInstance(innerException)));

    verify(mockException, never()).setHandled(anyBoolean());
  }

  /**
   * On fatal error, the exception strategies are not supposed to use Message.toString() as it could potentially log sensible
   * data.
   */
  @Test
  public void testMessageToStringNotCalledOnFailure() throws Exception {
    var message = spy(of(""));
    when(message.toString()).thenThrow(new RuntimeException("Message.toString() should not be called"));
    muleEvent = CoreEvent.builder(muleEvent).message(message).build();

    when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    onErrorPropagateHandler
        .setMessageProcessors(asList(createFailingEventMessageProcessor(new RuntimeException(mockException)),
                                     createFailingEventMessageProcessor(new RuntimeException(mockException))));
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);

    assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));
  }

  @Test
  public void subscriberContextPropagation() throws MuleException {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    onErrorPropagateHandler
        .setMessageProcessors(singletonList(contextPropagationChecker));

    initialiseIfNeeded(onErrorPropagateHandler, muleContext);

    AtomicReference<Throwable> thownRef = new AtomicReference<>();
    final Consumer<Exception> router = onErrorPropagateHandler
        .router(pub -> Flux.from(pub)
            .contextWrite(contextPropagationChecker.contextPropagationFlag()),
                e -> {
                },
                thownRef::set);

    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);
    router.accept(mockException);

    assertThat(thownRef.get().getCause(), not(instanceOf(AssertionError.class)));
    assertThat(thownRef.get(), sameInstance(mockException));
  }

  @Test
  public void exceptionRoutersAreDisposedWhenNoErrorInMono() throws MuleException {
    addErrorType(CONNECTIVITY);

    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    startIfNeeded(onErrorPropagateHandler);
    MessageProcessorChain chain =
        buildNewChainWithListOfProcessors(empty(), singletonList(createSetStringMessageProcessor("")), onErrorPropagateHandler);
    initialiseIfNeeded(chain, muleContext);
    just(testEvent()).transform(chain).block();
    onErrorPropagateHandler.assertAllRoutersWereDisposed();
    disposeIfNeeded(chain, LOGGER);
  }

  @Test
  public void exceptionRoutersAreDisposedWhenErrorInMono() throws MuleException {
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    startIfNeeded(onErrorPropagateHandler);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    addErrorType(CONNECTIVITY);

    MessageProcessorChain chain =
        buildNewChainWithListOfProcessors(empty(), singletonList(createFailingEventMessageProcessor(mockException)),
                                          onErrorPropagateHandler);
    initialiseIfNeeded(chain, muleContext);

    final org.mule.runtime.api.message.Error error = mock(org.mule.runtime.api.message.Error.class);
    when(error.getCause()).thenReturn(mockException);
    final ErrorType errorType = mockException.getExceptionInfo().getErrorType();
    when(error.getErrorType()).thenReturn(errorType);
    final CoreEvent event = CoreEvent.builder(testEvent())
        .error(error)
        .build();

    final Mono<CoreEvent> mono = just(event).transform(chain);
    var thrown = assertThrows(Exception.class, () -> mono.block());
    assertThat(thrown, hasRootCause(sameInstance(mockException)));

    onErrorPropagateHandler.assertAllRoutersWereDisposed();
    disposeIfNeeded(chain, getLogger(getClass()));
  }

  @Test
  @Feature(SUPPORTABILITY)
  @Story(ALERTS)
  public void unknownErrorAlertTriggered() throws Exception {
    addErrorType(UNKNOWN);
    when(mockException.handled()).thenReturn(false);
    when(mockException.getDetailedMessage()).thenReturn(DEFAULT_LOG_MESSAGE);
    when(mockException.getEvent()).thenReturn(muleEvent);
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    onErrorPropagateHandler.setAlertingSupport(alertingSupport);
    startIfNeeded(onErrorPropagateHandler);

    assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));

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
    initialiseIfNeeded(onErrorPropagateHandler, muleContext);
    onErrorPropagateHandler.setAlertingSupport(alertingSupport);
    startIfNeeded(onErrorPropagateHandler);

    assertThrows(Exception.class, () -> onErrorPropagateHandler.handleException(mockException, muleEvent));

    verify(alertingSupport, never()).triggerAlert(any());
    verify(alertingSupport, never()).triggerAlert(any(), any());
  }

  private Processor createChangingEventMessageProcessor(final CoreEvent lastEventCreated) {
    return event -> CoreEvent.builder(event)
        .message(lastEventCreated.getMessage())
        .variables(lastEventCreated.getVariables())
        .build();
  }

  private Processor createFailingEventMessageProcessor(Exception toThrow) {
    return event -> {
      if (toThrow instanceof MuleException muleExceptionToThrow) {
        throw muleExceptionToThrow;
      } else {
        throw new DefaultMuleException(toThrow);
      }
    };
  }

  private Processor createSetStringMessageProcessor(final String appendText) {
    return event -> CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).value(appendText).build())
        .build();
  }

  private void configureXaTransactionAndSingleResourceTransaction() throws TransactionException {
    TransactionCoordination.getInstance().bindTransaction(mockXaTransaction);
    TransactionCoordination.getInstance().suspendCurrentTransaction();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
  }

  private static class TestOnErrorPropagateHandler extends OnErrorPropagateHandler {

    private final Set<ExceptionRouter> allRouters = new HashSet<>();

    @Override
    public Consumer<Exception> router(Function<Publisher<CoreEvent>, Publisher<CoreEvent>> publisherPostProcessor,
                                      Consumer<CoreEvent> continueCallback, Consumer<Throwable> propagateCallback) {
      ExceptionRouter newRouter =
          spy((ExceptionRouter) super.router(publisherPostProcessor, continueCallback, propagateCallback));
      allRouters.add(newRouter);
      return newRouter;
    }

    void assertAllRoutersWereDisposed() {
      for (ExceptionRouter router : allRouters) {
        verify(router).dispose();
      }
    }
  }
}
