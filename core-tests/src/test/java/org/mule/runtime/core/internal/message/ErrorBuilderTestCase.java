/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import org.mule.runtime.api.component.Component;

import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
@Feature(ERROR_HANDLING)
public class ErrorBuilderTestCase extends AbstractMuleTestCase {

  private static final String EXCEPTION_MESSAGE = "message";
  private final ErrorType mockErrorType = mock(ErrorType.class);

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void errorCanNotBeNull() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("error exception cannot be null");
    builder().build();
  }

  @Test
  public void errorTypeCanNotBeNull() {
    RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("errorType exception cannot be null");
    builder(exception).build();
  }

  @Test
  @Issue("MULE-19408")
  public void errorCanBeSerializedByAnObjectOutputStreamEvenWhenFailingComponentIsNotSerializable() throws IOException {
    Component mockNonSerializableFailingComponent = mock(Component.class);
    when(mockNonSerializableFailingComponent.getRepresentation()).thenReturn("mock/representation");

    RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
    Error error = builder(exception).errorType(mockErrorType).failingComponent(mockNonSerializableFailingComponent).build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(error);
    }

    assertThat(baos.toString(), containsString("mock/representation"));
  }

  @Test
  @Issue("MULE-19408")
  @Description("An instance of the deprecated ErrorImplementation could be already serialized. We should be able to " +
      "deserialize it")
  public void alreadySerializedErrorImplementationCanBeDeserialized() throws IOException, ClassNotFoundException {
    ErrorBuilder.ErrorImplementation deprecated = getDeprecatedImpl();

    // Serialize a deprecated object.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(deprecated);
    }

    // Deserialize the object and check it's now a non-deprecated object.
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    try (ObjectInputStream ois = new ObjectInputStream(bais)) {
      Object o = ois.readObject();
      assertThat(o, instanceOf(ErrorBuilder.DeserializableErrorImplementation.class));
    }
  }

  private ErrorBuilder.ErrorImplementation getDeprecatedImpl() {
    Component mockSerializableFailingComponent = mock(SerializableComponent.class);
    RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
    Error anError = builder(exception).errorType(mockErrorType).build();
    return new ErrorBuilder.ErrorImplementation(anError.getCause(), anError.getDescription(), anError.getDetailedDescription(),
                                                mockSerializableFailingComponent, anError.getErrorType(),
                                                anError.getErrorMessage(), anError.getChildErrors());
  }

  @Test
  public void buildErrorFromException() {
    RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
    Error error = builder(exception).errorType(mockErrorType).build();
    assertThat(error.getCause(), is(exception));
    assertThat(error.getDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getDetailedDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getErrorType(), is(mockErrorType));
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
    assertThat(error.getChildErrors(), is(empty()));
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
    List<Error> childErrors = error.getChildErrors();
    assertThat(childErrors, hasSize(2));
    assertThat(childErrors.get(0).getCause(), is(instanceOf(RuntimeException.class)));
    assertThat(childErrors.get(1).getCause(), is(instanceOf(IOException.class)));
  }

  @Test
  public void givesStringRepresentation() {
    ErrorType anyError = ErrorTypeBuilder.builder().namespace("MULE").identifier("ANY").build();
    ErrorType testError = ErrorTypeBuilder.builder().namespace("MULE").identifier("TEST").parentErrorType(anyError).build();
    Error error = builder(new ComposedErrorMessageAwareException(createStaticMessage(EXCEPTION_MESSAGE), testError))
        .errorType(testError)
        .build();

    assertThat(error.toString(),
               is(equalToIgnoringLineBreaks("\norg.mule.runtime.core.internal.message.ErrorBuilder$DeserializableErrorImplementation\n"
                   + "{\n"
                   + "  description=message\n"
                   + "  detailedDescription=message\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=org.mule.runtime.core.internal.message.ErrorBuilderTestCase$ComposedErrorMessageAwareException\n"
                   + "  errorMessage=\n"
                   + "org.mule.runtime.core.internal.message.DefaultMessageBuilder$MessageImplementation\n"
                   + "{\n"
                   + "  payload=test\n"
                   + "  mediaType=*/*\n"
                   + "  attributes=<not set>\n"
                   + "  attributesMediaType=*/*\n"
                   + "}\n"
                   + "  childErrors=[\n"
                   + "org.mule.runtime.core.internal.message.ErrorBuilder$DeserializableErrorImplementation\n"
                   + "{\n"
                   + "  description=unknown description\n"
                   + "  detailedDescription=unknown description\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=java.lang.RuntimeException\n"
                   + "  errorMessage=-\n"
                   + "  childErrors=[]\n"
                   + "}, \n"
                   + "org.mule.runtime.core.internal.message.ErrorBuilder$DeserializableErrorImplementation\n"
                   + "{\n"
                   + "  description=unknown description\n"
                   + "  detailedDescription=unknown description\n"
                   + "  errorType=MULE:TEST\n"
                   + "  cause=java.io.IOException\n"
                   + "  errorMessage=-\n"
                   + "  childErrors=[]\n"
                   + "}]\n"
                   + "}")));
  }

  private class ComposedErrorMessageAwareException extends MuleException implements ErrorMessageAwareException,
      ComposedErrorException {

    private ErrorType errorType;

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

  private interface SerializableComponent extends Serializable, Component {
  }

}
