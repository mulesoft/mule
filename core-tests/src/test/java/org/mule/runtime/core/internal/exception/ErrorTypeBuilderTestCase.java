/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_TYPES;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ERROR_TYPES)
public class ErrorTypeBuilderTestCase extends AbstractMuleTestCase {

  private static final String NAMESPACE = "CUSTOM";
  private static final String IDENTIFIER = "MY_ERROR";
  private static final String REPRESENTATION = "CUSTOM:MY_ERROR";

  private ErrorTypeBuilder errorTypeBuilder = ErrorTypeBuilder.builder();
  private ErrorType mockErrorType = mock(ErrorType.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void parentMustBeSet() {
    expectException("parent error type cannot be null");
    errorTypeBuilder.namespace(NAMESPACE).identifier(IDENTIFIER).build();
  }

  @Test
  public void identifierMustBeSet() {
    expectException("string representation cannot be null");
    errorTypeBuilder.namespace(NAMESPACE).parentErrorType(mockErrorType).build();
  }

  @Test
  public void namespaceMustBeSet() {
    expectException("namespace representation cannot be null");
    errorTypeBuilder.identifier(IDENTIFIER).parentErrorType(mockErrorType).build();
  }

  @Test
  public void createsExpectedTypeAndRepresentation() {
    ErrorType errorType = errorTypeBuilder.namespace(NAMESPACE).identifier(IDENTIFIER).parentErrorType(mockErrorType).build();
    assertThat(errorType.getParentErrorType(), is(mockErrorType));
    assertThat(errorType.getNamespace(), is(NAMESPACE));
    assertThat(errorType.getIdentifier(), is(IDENTIFIER));
    assertThat(errorType.toString(), is(REPRESENTATION));
  }

  private void expectException(String exceptionMessage) {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(containsString(exceptionMessage));
  }

}
