/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.EXPRESSION;
import static org.mule.runtime.core.internal.exception.DefaultErrorTypeRepository.CRITICAL_ERROR_TYPE;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
@RunWith(MockitoJUnitRunner.class)
public class ErrorHandlerTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy1;

  @Mock
  private MessagingExceptionHandlerAcceptor mockTestExceptionStrategy2;
  private DefaultMessagingExceptionHandlerAcceptor defaultMessagingExceptionHandler =
      spy(new DefaultMessagingExceptionHandlerAcceptor());

  private CoreEvent event;

  private MuleContextWithRegistry mockMuleContext = mockContextWithServices();
  @Mock
  private ErrorType mockErrorType;
  private MessagingException mockException;

  @Before
  public void before() throws MuleException {
    Error mockError = mock(Error.class);
    when(mockError.getErrorType()).thenReturn(mockErrorType);

    event = getEventBuilder().message(Message.of("")).error(mockError).build();

    mockException = new MessagingException(event, new Exception());
    when(mockTestExceptionStrategy2.accept(any(CoreEvent.class))).thenReturn(true);
  }

  @Test
  public void secondMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    when(mockMuleContext.getDefaultErrorHandler(empty())).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setRootContainerName("root");
    errorHandler.initialise();
    when(mockTestExceptionStrategy1.accept(any(CoreEvent.class))).thenReturn(false);
    errorHandler.handleException(mockException, event);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(0)).apply(any(MessagingException.class));
    verify(mockTestExceptionStrategy2, times(1)).handleException(eq(mockException), any(CoreEvent.class));
  }

  @Test
  public void firstAcceptsAllMatches() throws Exception {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1, mockTestExceptionStrategy2)));
    errorHandler.setMuleContext(mockMuleContext);
    when(mockMuleContext.getDefaultErrorHandler(empty())).thenReturn(defaultMessagingExceptionHandler);
    when(mockTestExceptionStrategy1.acceptsAll()).thenReturn(true);
    when(mockTestExceptionStrategy2.acceptsAll()).thenReturn(false);
    errorHandler.setRootContainerName("root");
    expectedException
        .expectMessage(containsString("Only last <on-error> inside <error-handler> can accept any errors."));
    errorHandler.initialise();
  }

  @Test
  public void criticalIsNotHandled() throws Exception {
    when(mockErrorType.getParentErrorType()).thenReturn(CRITICAL_ERROR_TYPE);
    ErrorHandler errorHandler = new ErrorHandler();
    when(mockMuleContext.getDefaultErrorHandler(empty())).thenReturn(defaultMessagingExceptionHandler);
    errorHandler.setExceptionListeners(new ArrayList<>(asList(mockTestExceptionStrategy1)));
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setRootContainerName("root");
    errorHandler.initialise();
    errorHandler.handleException(mockException, event);
    verify(mockTestExceptionStrategy1, times(0)).apply(any(MessagingException.class));
    verify(defaultMessagingExceptionHandler, times(0)).apply(any(MessagingException.class));
  }

  @Test
  public void defaultErrorHandler() throws InitialisationException {
    final ErrorHandler defaultHandler = new ErrorHandlerFactory().createDefault(mock(NotificationDispatcher.class));

    assertThat(defaultHandler.getExceptionListeners(), hasSize(1));
    assertThat(defaultHandler.getExceptionListeners(), hasItem(instanceOf(OnErrorPropagateHandler.class)));
  }

  @Test
  public void ifUserDefaultIsMissingCatchAllThenGetsInjectedOurDefault() throws Exception {
    verifyInjectionWithConfiguredOnError(mockTestExceptionStrategy1);
  }

  @Test
  public void ifUserDefaultHasPropagateSpecificThenGetsInjectedOurDefault() throws Exception {
    OnErrorPropagateHandler onError = new OnErrorPropagateHandler();
    onError.setErrorType("EXPRESSION");

    verifyInjectionWithConfiguredOnError(onError);
  }

  @Test
  public void ifUserDefaultHasPropagateAnyThenOurDefaultIsNotInjected() throws Exception {
    OnErrorPropagateHandler onError = new OnErrorPropagateHandler();
    onError.setErrorType("ANY");

    verifyNoInjectionWithConfiguredOnError(onError);
  }

  @Test
  public void ifUserDefaultHasEmptyPropagateThenOurDefaultIsNotInjected() throws Exception {
    verifyNoInjectionWithConfiguredOnError(new OnErrorPropagateHandler());
  }

  private void verifyInjectionWithConfiguredOnError(MessagingExceptionHandlerAcceptor onError) throws InitialisationException {
    ErrorHandler errorHandler = prepareErrorHandler(onError);

    assertThat(errorHandler.getExceptionListeners(), hasSize(3));
    assertThat(errorHandler.getExceptionListeners().get(0), is(instanceOf(OnCriticalErrorHandler.class)));
    assertThat(errorHandler.getExceptionListeners().get(1), is(onError));
    MessagingExceptionHandlerAcceptor injected = errorHandler.getExceptionListeners().get(2);
    assertThat(injected, is(instanceOf(OnErrorPropagateHandler.class)));
  }

  private void verifyNoInjectionWithConfiguredOnError(MessagingExceptionHandlerAcceptor onError) throws InitialisationException {
    ErrorHandler errorHandler = prepareErrorHandler(onError);

    assertThat(errorHandler.getExceptionListeners(), hasSize(2));
    assertThat(errorHandler.getExceptionListeners().get(0), is(instanceOf(OnCriticalErrorHandler.class)));
    assertThat(errorHandler.getExceptionListeners().get(1), is(onError));
  }

  private ErrorHandler prepareErrorHandler(MessagingExceptionHandlerAcceptor onError) throws InitialisationException {
    ErrorHandler errorHandler = new ErrorHandler();
    List<MessagingExceptionHandlerAcceptor> handlerList = new LinkedList<>();
    handlerList.add(onError);
    errorHandler.setExceptionListeners(handlerList);
    errorHandler.setMuleContext(mockMuleContext);
    errorHandler.setRootContainerName("root");
    errorHandler.setName("myDefault");

    when(mockMuleContext.getConfiguration().getDefaultErrorHandlerName()).thenReturn("myDefault");
    when(mockMuleContext.getDefaultErrorHandler(of("root"))).thenReturn(errorHandler);
    mockErrorRepository();
    errorHandler.initialise();
    return errorHandler;
  }

  private void mockErrorRepository() {
    ErrorTypeRepository errorTypeRepository = mock(ErrorTypeRepository.class, RETURNS_DEEP_STUBS);
    when(mockMuleContext.getErrorTypeRepository()).thenReturn(errorTypeRepository);
    ErrorType anyErrorType = mock(ErrorType.class);
    when(errorTypeRepository.lookupErrorType(ANY)).thenReturn(of(anyErrorType));
    when(errorTypeRepository.lookupErrorType(EXPRESSION)).thenReturn(of(mock(ErrorType.class)));
    when(errorTypeRepository.getErrorType(any(ComponentIdentifier.class))).thenReturn(of(mock(ErrorType.class)));
    when(errorTypeRepository.getAnyErrorType()).thenReturn(anyErrorType);
  }

  class DefaultMessagingExceptionHandlerAcceptor implements MessagingExceptionHandlerAcceptor {

    @Override
    public boolean accept(CoreEvent event) {
      return true;
    }

    @Override
    public boolean acceptsAll() {
      return true;
    }

    @Override
    public CoreEvent handleException(Exception exception, CoreEvent event) {
      if (exception instanceof MessagingException) {
        ((MessagingException) exception).setHandled(true);
      }
      return event;
    }
  }

}
