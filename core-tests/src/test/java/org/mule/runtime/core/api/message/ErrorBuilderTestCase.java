/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.internal.exception.SuppressedMuleException.suppressIfPresent;
import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.config.internal.error.ErrorTypeBuilder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.ExceptionUtils;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.message.PrivilegedError;
import org.mule.runtime.internal.exception.SuppressedMuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

@SmallTest
@Feature(ERROR_HANDLING)
public class ErrorBuilderTestCase extends AbstractMuleTestCase {

  private static final String EXCEPTION_MESSAGE = "message";
  private static final String SUPPRESSED_EXCEPTION_MESSAGE = "suppressed message";
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
    assertThat(((PrivilegedError) error).getSuppressedErrors(), is(empty()));
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
    assertThat(((PrivilegedError) error).getSuppressedErrors(), is(empty()));
    assertThat(error.getChildErrors(), is(empty()));
  }

  @Test
  @Issue("MULE-18562")
  public void buildErrorFromMuleExceptionWithSuppression() {
    MessagingException messagingException = getMessagingException();
    Throwable exception =
        suppressIfPresent(new DefaultMuleException(EXCEPTION_MESSAGE, messagingException), MessagingException.class);
    buildErrorAndAssertSuppression(exception, messagingException);
  }

  @Test
  @Issue("MULE-18562")
  public void buildErrorFromMuleExceptionWithNestedSuppression() {
    MessagingException messagingException = getMessagingException();
    Throwable exception =
        new DefaultMuleException(EXCEPTION_MESSAGE, suppressIfPresent(messagingException, MessagingException.class));
    buildErrorAndAssertSuppression(exception, messagingException);
  }

  @Test
  @Issue("MULE-18562")
  public void buildErrorFromComposedErrorMessageAwareExceptionWithSuppression() {
    ComposedErrorMessageAwareException composedMessageAwareException =
        new ComposedErrorMessageAwareException(createStaticMessage(EXCEPTION_MESSAGE), testError, getMessagingException());
    // Adding a suppression in order to wrap the ComposedErrorMessageAwareException
    Error error = builder(SuppressedMuleException.suppressIfPresent(composedMessageAwareException, MessagingException.class))
        .errorType(testError)
        .build();
    assertThat(error.getDetailedDescription(), equalTo(EXCEPTION_MESSAGE));
    // Backwards compatibility demands that the description has to be set to the suppressed exception message
    assertThat(error.getDescription(), equalTo(SUPPRESSED_EXCEPTION_MESSAGE));
    assertThat(((PrivilegedError) error).getSuppressedErrors(), hasSize(1));
    Throwable suppressedErrorCause = ((PrivilegedError) error).getSuppressedErrors().get(0).getCause();
    assertThat(suppressedErrorCause, instanceOf(TestMuleException.class));
    // Backwards compatibility demands that the error cause has to be the suppressed exception
    assertThat(error.getCause(), equalTo(suppressedErrorCause));
    // ComposedErrorMessageAwareException must be treated by the error builder
    assertThat(error.getErrorMessage().getPayload().getValue().toString(), equalTo(TEST_PAYLOAD));
    assertThat(error.getChildErrors(), contains(composedMessageAwareException.getErrors().toArray()));
  }

  @Test
  public void buildError() {
    String detailedDescription = "detailed description";
    String description = "description";
    ErrorType errorType = mockErrorType;
    Message errorMessage = of(null);
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
    assertThat(((PrivilegedError) error).getSuppressedErrors(), is(empty()));
    List<Error> childErrors = error.getChildErrors();
    assertThat(childErrors, hasSize(2));
    assertThat(childErrors.get(0).getCause(), is(instanceOf(RuntimeException.class)));
    assertThat(childErrors.get(1).getCause(), is(instanceOf(IOException.class)));
  }

  @Test
  public void givesStringRepresentation() {
    Error error = getComposedErrorMessageAwareWithSuppressionException();
    assertThat(error.toString(),
               is(equalToIgnoringLineBreaks("\norg.mule.runtime.core.internal.message.ErrorBuilder$ErrorImplementation\n"
                   + "{\n"
                   + "  description=suppressed message\n"
                   + "  detailedDescription=message\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=org.mule.runtime.core.api.message.ErrorBuilderTestCase$TestMuleException\n"
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
                   + "  description=suppressed message\n"
                   + "  detailedDescription=suppressed message\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=org.mule.runtime.core.api.message.ErrorBuilderTestCase$TestMuleException\n"
                   + "  errorMessage=-\n"
                   + "  suppressedErrors=[]\n"
                   + "  childErrors=[]\n"
                   + "}]\n"
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

  private Error getComposedErrorMessageAwareWithSuppressionException() {
    Throwable composedMessageAwareException =
        new ComposedErrorMessageAwareException(createStaticMessage(EXCEPTION_MESSAGE), testError, getMessagingException());
    composedMessageAwareException =
        SuppressedMuleException.suppressIfPresent(composedMessageAwareException, MessagingException.class);
    return builder(composedMessageAwareException)
        .errorType(testError)
        .build();
  }

  private void buildErrorAndAssertSuppression(Throwable exception, MessagingException suppressedCause) {
    Error error = builder(exception).errorType(mockErrorType).build();
    assertThat(error.getDescription(), containsString(suppressedCause.getMessage()));
    assertThat(error.getDetailedDescription(), containsString(EXCEPTION_MESSAGE));
    assertThat(error.getErrorType(), is(mockErrorType));
    assertThat(error.getCause(), is(getMessagingExceptionCause(suppressedCause)));
    assertThat(((PrivilegedError) error).getSuppressedErrors(), contains(suppressedCause.getEvent().getError().get()));
    assertThat(error.getChildErrors(), is(empty()));
  }

  private MessagingException getMessagingException() {
    CoreEvent suppressedErrorEvent = mock(CoreEvent.class);
    MuleException messagingExceptionCause = new TestMuleException(createStaticMessage(SUPPRESSED_EXCEPTION_MESSAGE));
    Error suppressedError = builder(messagingExceptionCause).errorType(testError).build();
    MessagingException messagingException =
        new MessagingException(suppressedErrorEvent, messagingExceptionCause);
    when(suppressedErrorEvent.getError()).thenReturn(Optional.of(suppressedError));
    return messagingException;
  }

  private class TestMuleException extends MuleException {

    public TestMuleException(I18nMessage message) {
      super(message);
    }
  }

  private class ComposedErrorMessageAwareException extends MuleException implements ErrorMessageAwareException,
      ComposedErrorException {

    private static final long serialVersionUID = -5454799054976232938L;
    private final List<Error> errors;

    public ComposedErrorMessageAwareException(I18nMessage message) {
      this(message, mockErrorType);
    }

    public ComposedErrorMessageAwareException(I18nMessage message, ErrorType errorType) {
      super(message);
      errors = populateErrors(errorType);
    }

    public ComposedErrorMessageAwareException(I18nMessage message, ErrorType errorType, Throwable cause) {
      super(message, cause);
      errors = populateErrors(errorType);
    }

    private List<Error> populateErrors(ErrorType errorType) {
      return asList(builder(new RuntimeException()).errorType(errorType).build(),
                    builder(new IOException()).errorType(errorType).build());
    }

    @Override
    public List<Error> getErrors() {
      return errors;
    }

    @Override
    public Message getErrorMessage() {
      return of(TEST_PAYLOAD);
    }

  }

}
