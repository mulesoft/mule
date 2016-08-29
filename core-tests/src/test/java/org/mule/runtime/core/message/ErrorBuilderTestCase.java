/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.message.ErrorBuilder.builder;

import org.junit.Test;
import org.mockito.Mockito;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class ErrorBuilderTestCase extends AbstractMuleTestCase {

  private static final String EXCEPTION_MESSAGE = "message";
  private final ErrorType mockErrorType = Mockito.mock(ErrorType.class);

  @Test
  public void buildErrorFromException() {
    RuntimeException exception = new RuntimeException(EXCEPTION_MESSAGE);
    Error error = builder(exception).errorType(mockErrorType).build();
    assertThat(error.getException(), is(exception));
    assertThat(error.getDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getDetailedDescription(), is(EXCEPTION_MESSAGE));
    assertThat(error.getErrorType(), is(mockErrorType));
  }

  @Test
  public void buildErrorFromMuleException() {
    MuleException exception = new DefaultMuleException(new RuntimeException(EXCEPTION_MESSAGE));
    Error error = builder(exception).errorType(mockErrorType).build();
    assertThat(error.getException(), is(exception));
    assertThat(error.getDescription(), containsString(EXCEPTION_MESSAGE));
    assertThat(error.getDetailedDescription(), containsString(EXCEPTION_MESSAGE));
    assertThat(error.getErrorType(), is(mockErrorType));
  }

  @Test
  public void buildError() {
    String detailedDescription = "detailed description";
    String description = "description";
    ErrorType errorType = mockErrorType;
    MuleMessage errorMessage = MuleMessage.builder().nullPayload().build();
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
    assertThat(error.getException(), is(exception));
    assertThat(error.getErrorType(), is(errorType));
    assertThat(error.getErrorMessage(), is(errorMessage));
  }



}
