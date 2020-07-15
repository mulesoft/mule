/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.internal.exception.SuppressedMuleException.suppressIfPresent;
import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;

import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

@SmallTest
public class ErrorBuilderTestCase extends AbstractMuleTestCase {

  private static final String EXCEPTION_MESSAGE = "message";
  private final ErrorType mockErrorType = mock(ErrorType.class);
  private final ErrorType anyError = ErrorTypeBuilder.builder().namespace("MULE").identifier("ANY").build();
  private final ErrorType testError =
      ErrorTypeBuilder.builder().namespace("MULE").identifier("TEST").parentErrorType(anyError).build();


  @Test
  public void buildErrorFromException() {
    RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
    Error error = builder(exception).errorType(mockErrorType).build();
    assertThat(error.getCause(), is(exception));
    assertThat(error.getDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getDetailedDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getErrorType(), is(mockErrorType));
    assertThat(error.getSuppressedErrors(), is(empty()));
    assertThat(error.getChildErrors(), is(empty()));
  }

  @Test
  public void buildErrorFromMuleException() {
    MuleException exception = new DefaultMuleException(new RuntimeException(EXCEPTION_MESSAGE));
    Error error = builder(exception).errorType(mockErrorType).build();
    assertThat(error.getCause(), is(exception));
    assertThat(error.getDescription(), containsString(EXCEPTION_MESSAGE));
    assertThat(error.getDetailedDescription(), containsString(EXCEPTION_MESSAGE));
    assertThat(error.getErrorType(), is(mockErrorType));
    assertThat(error.getSuppressedErrors(), is(empty()));
    assertThat(error.getChildErrors(), is(empty()));
  }

  @Test
  public void buildErrorWithSuppressedError() {
    MessagingException messagingException = getMessagingException();
    Throwable exception =
        suppressIfPresent(new DefaultMuleException(EXCEPTION_MESSAGE, messagingException), MessagingException.class);
    buildErrorAndAssertSuppression(exception, messagingException);
  }

  @Test
  public void buildErrorWithNestedSuppressedError() {
    MessagingException messagingException = getMessagingException();
    Throwable exception =
        new DefaultMuleException(EXCEPTION_MESSAGE, suppressIfPresent(messagingException, MessagingException.class));
    buildErrorAndAssertSuppression(exception, messagingException);
  }

  @Test
  public void buildError() {
    String detailedDescription = "detailed description";
    String description = "description";
    ErrorType errorType = mockErrorType;
    Message errorMessage = Message.of(null);
    IllegalArgumentException exception = new IllegalArgumentException("some message");
    Error error = builder()
        .errorType(errorType)
        .description(description)
        .detailedDescription(detailedDescription)
        .exception(exception)
        .errorMessage(errorMessage)
        .build();
    assertThat(error.getDescription(), is(description));
    assertThat(error.getDetailedDescription(), is(detailedDescription));
    assertThat(error.getCause(), is(exception));
    assertThat(error.getErrorType(), is(errorType));
    assertThat(error.getErrorMessage(), is(errorMessage));
    assertThat(error.getChildErrors(), is(empty()));
  }

  @Test
  public void buildErrorFromCustomException() {
    Error error = builder(new ComposedErrorMessageAwareException(createStaticMessage(EXCEPTION_MESSAGE)))
        .errorType(mockErrorType)
        .build();

    assertThat(error.getDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getDetailedDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getCause(), is(instanceOf(ComposedErrorMessageAwareException.class)));
    assertThat(error.getErrorType(), is(mockErrorType));
    assertThat(error.getErrorMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(error.getSuppressedErrors(), is(empty()));
    List<Error> childErrors = error.getChildErrors();
    assertThat(childErrors, hasSize(2));
    assertThat(childErrors.get(0).getCause(), is(instanceOf(RuntimeException.class)));
    assertThat(childErrors.get(1).getCause(), is(instanceOf(IOException.class)));
  }

  @Test
  public void givesStringRepresentation() {
    MuleException composedMessageAwareException =
        new ComposedErrorMessageAwareException(createStaticMessage(EXCEPTION_MESSAGE), testError);
    composedMessageAwareException.getExceptionInfo().addSuppressedCause(getMessagingException());
    Error error = builder(composedMessageAwareException)
        .errorType(testError)
        .build();

    assertThat(error.toString(),
               is(equalToIgnoringLineBreaks("\norg.mule.runtime.core.internal.message.ErrorBuilder$ErrorImplementation\n"
                   + "{\n"
                   + "  description=message\n"
                   + "  detailedDescription=message\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=org.mule.runtime.core.api.message.ErrorBuilderTestCase$ComposedErrorMessageAwareException\n"
                   + "  errorMessage=\n"
                   + "org.mule.runtime.core.internal.message.DefaultMessageBuilder$MessageImplementation\n"
                   + "{\n"
                   + "  payload=test\n"
                   + "  mediaType=*/*\n"
                   + "  attributes=<not set>\n"
                   + "  attributesMediaType=*/*\n"
                   + "}\n"
                   + "  suppressedErrors=[\n"
                   + "org.mule.runtime.core.internal.message.ErrorBuilder$ErrorImplementation\n"
                   + "{\n"
                   + "  description=Test\n"
                   + "  detailedDescription=Test\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=org.mule.runtime.core.internal.exception.MessagingException\n"
                   + "  errorMessage=-\n"
                   + "  suppressedErrors=[]\n"
                   + "  childErrors=[]\n"
                   + "}]"
                   + "  childErrors=[\n"
                   + "org.mule.runtime.core.internal.message.ErrorBuilder$ErrorImplementation\n"
                   + "{\n"
                   + "  description=unknown description\n"
                   + "  detailedDescription=unknown description\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=java.lang.RuntimeException\n"
                   + "  errorMessage=-\n"
                   + "  suppressedErrors=[]\n"
                   + "  childErrors=[]\n"
                   + "}, \n"
                   + "org.mule.runtime.core.internal.message.ErrorBuilder$ErrorImplementation\n"
                   + "{\n"
                   + "  description=unknown description\n"
                   + "  detailedDescription=unknown description\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=java.io.IOException\n"
                   + "  errorMessage=-\n"
                   + "  suppressedErrors=[]\n"
                   + "  childErrors=[]\n"
                   + "}]\n"
                   + "}")));
  }

  private void buildErrorAndAssertSuppression(Throwable exception, MessagingException suppressedCause) {
    Error error = builder(exception).errorType(mockErrorType).build();
    assertThat(error.getCause(), is(exception));
    assertThat(error.getDescription(), containsString(EXCEPTION_MESSAGE));
    assertThat(error.getDetailedDescription(), containsString(EXCEPTION_MESSAGE));
    assertThat(error.getErrorType(), is(mockErrorType));
    assertThat(error.getSuppressedErrors(), contains(suppressedCause.getEvent().getError().get()));
    assertThat(error.getChildErrors(), is(empty()));
  }

  private MessagingException getMessagingException() {
    CoreEvent suppressedErrorEvent = mock(CoreEvent.class);
    MessagingException suppressedMessagingException = new MessagingException(createStaticMessage("Test"), suppressedErrorEvent);
    Error suppressedError = builder(suppressedMessagingException).errorType(testError).build();
    when(suppressedErrorEvent.getError()).thenReturn(of(suppressedError));
    return suppressedMessagingException;
  }

  private class ComposedErrorMessageAwareException extends MuleException implements ErrorMessageAwareException,
      ComposedErrorException {

    private static final long serialVersionUID = -5454799054976232938L;

    private final ErrorType errorType;

    public ComposedErrorMessageAwareException(I18nMessage message) {
      this(message, mockErrorType);
    }

    public ComposedErrorMessageAwareException(I18nMessage message, ErrorType errorType) {
      super(message);
      this.errorType = errorType;
    }

    @Override
    public List<Error> getErrors() {
      return asList(builder(new RuntimeException()).errorType(errorType).build(),
                    builder(new IOException()).errorType(errorType).build());
    }

    @Override
    public Message getErrorMessage() {
      return Message.of(TEST_PAYLOAD);
    }

  }

}
