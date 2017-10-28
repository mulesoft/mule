/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.exception.DefaultErrorTypeRepository.CRITICAL_ERROR_TYPE;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(ERROR_HANDLING)
public class OnCriticalErrorHandlerTestCase extends AbstractErrorHandlerTestCase {

  private OnCriticalErrorHandler handler = spy(new OnCriticalErrorHandler());

  private Error error;


  public OnCriticalErrorHandlerTestCase(SystemProperty verbose) {
    super(verbose);
  }

  @Before
  public void before() throws Exception {
    super.before();

    error = mock(Error.class, RETURNS_DEEP_STUBS);

    muleEvent = InternalEvent.builder(muleEvent).error(error).build();
  }

  @Test
  public void logsException() {
    when(mockException.getDetailedMessage()).thenReturn("Log");
    when(mockException.getEvent()).thenReturn(muleEvent);
    handler.apply(mockException);
    handler.handleException(mockException, muleEvent);
    verify(handler, times(2)).logException(any(MessagingException.class));
  }

  @Test
  public void acceptsCritical() {
    when(error.getErrorType()).thenReturn(CRITICAL_ERROR_TYPE);

    assertThat(handler.accept(muleEvent), is(true));
  }

  @Test
  public void acceptsCriticalChild() {
    ErrorType errorType = mock(ErrorType.class);
    when(error.getErrorType()).thenReturn(errorType);
    when(errorType.getParentErrorType()).thenReturn(CRITICAL_ERROR_TYPE);

    assertThat(handler.accept(muleEvent), is(true));
  }

}
