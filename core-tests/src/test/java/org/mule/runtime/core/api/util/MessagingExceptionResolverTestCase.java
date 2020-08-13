/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.exception.MuleExceptionInfo.INFO_CAUSED_BY_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.internal.exception.SuppressedMuleException.suppressIfPresent;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.ExceptionMapper;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory;
import org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.connector.DispatchException;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.tck.integration.transformer.ValidateResponse;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;

@SmallTest
@Feature(ERROR_HANDLING)
public class MessagingExceptionResolverTestCase extends AbstractMuleTestCase {

  private static final String ERROR_MESSAGE = "Messaging Error Message";
  private static final String EXPECTED_MESSAGE = "THIS MESSAGE SHOULD BE THROWN";

  private final Component processor = mock(Component.class);
  private CoreEvent event;

  private final Message message = mock(Message.class);
  private final ComponentIdentifier ci = mock(ComponentIdentifier.class);

  private final TransformerException TRANSFORMER_EXCEPTION = new TransformerException(createStaticMessage("TRANSFORMER"));

  private final ConnectionException CONNECTION_EXCEPTION = new ConnectionException("CONNECTION PROBLEM");
  private final MuleFatalException FATAL_EXCEPTION = new MuleFatalException(createStaticMessage("CRITICAL!!!!!!"));
  private final java.lang.Error ERROR = new java.lang.Error("AN ERROR");

  private final ErrorTypeRepository repository = ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository();
  private final ErrorTypeLocator locator = ErrorTypeLocatorFactory.createDefaultErrorTypeLocator(repository);

  private final ErrorType UNKNOWN = locator.lookupErrorType(Exception.class);
  private final ErrorType FATAL = locator.lookupErrorType(FATAL_EXCEPTION.getClass());
  private final ErrorType CRITICAL = locator.lookupErrorType(ERROR.getClass());
  private final ErrorType CONNECTION = locator.lookupErrorType(CONNECTION_EXCEPTION.getClass());
  private final ErrorType TRANSFORMER = locator.lookupErrorType(TRANSFORMER_EXCEPTION.getClass());
  private final ErrorType DISPATCH = locator.lookupErrorType(DispatchException.class);


  private final MessagingExceptionResolver resolver = new MessagingExceptionResolver(processor);

  @Before
  public void setup() throws MuleException {
    when(message.getPayload()).thenReturn(new TypedValue<>(null, DataType.STRING));
    when(message.getAttributes()).thenReturn(new TypedValue<>(null, DataType.STRING));
    event = spy(getEventBuilder().message(message).build());
  }

  @Test
  public void resolveExceptionWithCriticalUnderlyingError() {
    Optional<Error> surfaceError = mockError(CONNECTION, null);
    when(event.getError()).thenReturn(surfaceError);
    MessagingException me = newMessagingException(ERROR, event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, CONNECTION);
  }

  @Test
  public void resolveExceptionWithUnknownUnderlyingError() {
    Optional<Error> surfaceError = mockError(CONNECTION, null);
    when(event.getError()).thenReturn(surfaceError);
    MessagingException me = newMessagingException(new Exception(), event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, CONNECTION);
  }

  @Test
  public void resolveWithoutAnyErrors() {
    MessagingException me = newMessagingException(new Exception(), event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, UNKNOWN);
    assertExceptionMessage(resolved.getMessage(), ERROR_MESSAGE);
  }

  @Test
  public void resolveCriticalError() {
    MessagingException me = newMessagingException(ERROR, event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, CRITICAL);
    assertExceptionMessage(resolved.getMessage(), ERROR.getMessage());
  }

  @Test
  public void resolveMultipleCriticalErrors() {
    Throwable t = new LinkageError("this one is NOT expected", new java.lang.Error(new java.lang.Error("expected")));
    MessagingException me = newMessagingException(t, event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, CRITICAL);
    assertExceptionMessage(resolved.getMessage(), "expected");
  }

  @Test
  public void resolveWithAnEventThatCarriesError() {
    Optional<Error> surfaceError = mockError(TRANSFORMER, null);
    when(event.getError()).thenReturn(surfaceError);
    MessagingException me = newMessagingException(new Exception(), event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, TRANSFORMER);
    assertExceptionMessage(resolved.getMessage(), ERROR_MESSAGE);
  }

  @Test
  public void resolveWithMultipleErrors() {
    Optional<Error> surfaceError = mockError(TRANSFORMER, TRANSFORMER_EXCEPTION);
    when(event.getError()).thenReturn(surfaceError);
    Exception cause = new Exception(new ConnectionException(FATAL_EXCEPTION));
    MessagingException me = newMessagingException(cause, event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, FATAL);
    assertExceptionMessage(resolved.getMessage(), FATAL_EXCEPTION.getMessage());
  }

  @Test
  public void resolveTopExceptionWithSameError() {
    Optional<Error> surfaceError = mockError(TRANSFORMER, TRANSFORMER_EXCEPTION);
    when(event.getError()).thenReturn(surfaceError);
    Exception cause = new MuleFatalException(createStaticMessage(EXPECTED_MESSAGE), new LinkageError("!"));
    MessagingException me = newMessagingException(cause, event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, FATAL);
    assertExceptionMessage(resolved.getMessage(), EXPECTED_MESSAGE);
  }

  @Test
  public void resolveWithParentInChain() {
    ErrorType withParent = ErrorTypeBuilder.builder().parentErrorType(CONNECTION).identifier("CONNECT").namespace("TEST").build();
    Optional<Error> surfaceError = mockError(withParent, new Exception());
    when(event.getError()).thenReturn(surfaceError);
    Exception cause = new ConnectionException("Some Connection Error", new Exception());
    MessagingException me = newMessagingException(cause, event, processor);
    MessagingException resolved = resolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, withParent);
    assertExceptionMessage(resolved.getMessage(), ERROR_MESSAGE);
  }

  @Test
  public void resolveCorrectConnectionException() {
    ErrorType expected = ErrorTypeBuilder.builder().namespace("NS").identifier("CONNECTION").parentErrorType(CONNECTION).build();
    ErrorTypeLocator locator = ErrorTypeLocator.builder(repository)
        .addComponentExceptionMapper(ci, ExceptionMapper.builder()
            .addExceptionMapping(ConnectionException.class, expected)
            .build())
        .defaultExceptionMapper(ExceptionMapper.builder().build())
        .defaultError(UNKNOWN)
        .build();
    MessagingException me = newMessagingException(CONNECTION_EXCEPTION, event, processor);
    MessagingExceptionResolver anotherResolver = new MessagingExceptionResolver(new TestProcessor());
    MessagingException resolved = anotherResolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, expected);
    assertExceptionMessage(resolved.getMessage(), "CONNECTION PROBLEM");
  }

  @Test
  @Issue("MULE-18041")
  public void resolveSuppressedMuleException() {
    ErrorType expected = DISPATCH;
    Throwable exception = new DispatchException(createStaticMessage("DISPATCH PROBLEM"), new ValidateResponse(),
                                                suppressIfPresent(CONNECTION_EXCEPTION,
                                                                  CONNECTION_EXCEPTION.getClass()));
    MessagingException me = newMessagingException(exception, event, processor);
    MessagingExceptionResolver anotherResolver = new MessagingExceptionResolver(new TestProcessor());
    MessagingException resolved = anotherResolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, expected);
    assertExceptionMessage(resolved.getMessage(), "DISPATCH PROBLEM");
    assertExceptionMessage(resolved.getInfo().get(INFO_CAUSED_BY_KEY).toString(), "CONNECTION PROBLEM");
  }

  @Test
  @Issue("MULE-18041")
  public void resolveSuppressedMuleExceptionLoggingCause() {
    ErrorType expected = DISPATCH;
    Throwable exception = new DispatchException(createStaticMessage("DISPATCH PROBLEM"), new ValidateResponse(),
                                                suppressIfPresent(CONNECTION_EXCEPTION,
                                                                  CONNECTION_EXCEPTION.getClass()));
    MessagingException me = newMessagingException(exception, event, processor);
    MessagingExceptionResolver anotherResolver = new MessagingExceptionResolver(new TestProcessor());
    MessagingException resolved = anotherResolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, expected);
    assertExceptionMessage(resolved.getMessage(), "DISPATCH PROBLEM");
    assertExceptionMessage(resolved.getInfo().get(INFO_CAUSED_BY_KEY).toString(), "CONNECTION PROBLEM");
  }

  @Test
  @Issue("MULE-18041")
  public void resolveSuppressedMessagingExceptionLoggingCause() {
    ErrorType expected = DISPATCH;
    Throwable exception = new DispatchException(createStaticMessage("DISPATCH PROBLEM"), new ValidateResponse(),
                                                suppressIfPresent(new MessagingException(createStaticMessage("CONNECTION PROBLEM"),
                                                                                         event),
                                                                  MessagingException.class));
    MessagingException me = newMessagingException(exception, event, processor);
    MessagingExceptionResolver anotherResolver = new MessagingExceptionResolver(new TestProcessor());
    MessagingException resolved = anotherResolver.resolve(me, locator, emptyList());
    assertExceptionErrorType(resolved, expected);
    assertExceptionMessage(resolved.getMessage(), "DISPATCH PROBLEM");
    assertExceptionMessage(resolved.getInfo().get(INFO_CAUSED_BY_KEY).toString(), "CONNECTION PROBLEM");
  }

  private void assertExceptionMessage(String result, String expected) {
    assertThat(result, containsString(expected));
  }

  private void assertExceptionErrorType(MessagingException me, ErrorType expected) {
    Optional<Error> error = me.getEvent().getError();
    assertThat("No error found, expecting error with error type [" + expected + "]", error.isPresent(), is(true));
    assertThat(error.get().getErrorType(), is(expected));
  }

  private MessagingException newMessagingException(Throwable e, CoreEvent event, Component processor) {
    return new MessagingException(createStaticMessage(ERROR_MESSAGE), event, e, processor);
  }

  private Optional<Error> mockError(ErrorType errorType, Throwable cause) {
    Error error = mock(Error.class);
    when(error.getErrorType()).thenReturn(errorType);
    when(error.getCause()).thenReturn(cause);
    return Optional.of(error);
  }

  public class TestProcessor implements AnnotatedProcessor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return nullPayloadEvent();
    }

    @Override
    public Object getAnnotation(QName name) {
      return ANNOTATION_NAME.equals(name) ? ci : null;
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return emptyMap();
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {}

    @Override
    public ComponentLocation getLocation() {
      return null;
    }

    @Override
    public Location getRootContainerLocation() {
      return null;
    }
  }
}
