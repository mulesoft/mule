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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.exception.DefaultErrorTypeRepository.CRITICAL_ERROR_TYPE;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(ERROR_HANDLING)
public class OnCriticalErrorHandlerTestCase extends AbstractMuleTestCase {

  private OnCriticalErrorHandler handler = spy(new OnCriticalErrorHandler());

  @Test
  public void logsException() {
    MessagingException messagingException = mock(MessagingException.class);
    when(messagingException.getDetailedMessage()).thenReturn("Log");
    handler.apply(messagingException);
    handler.handleException(messagingException, mock(CoreEvent.class));
    verify(handler, times(2)).logException(any(MessagingException.class));
  }

  @Test
  public void acceptsCritical() {
    CoreEvent event = mock(CoreEvent.class);
    Error error = mock(Error.class);
    when(event.getError()).thenReturn(Optional.of(error));
    when(error.getErrorType()).thenReturn(CRITICAL_ERROR_TYPE);

    assertThat(handler.accept(event), is(true));
  }

  @Test
  public void acceptsCriticalChild() {
    CoreEvent event = mock(CoreEvent.class);
    Error error = mock(Error.class);
    when(event.getError()).thenReturn(Optional.of(error));
    ErrorType errorType = mock(ErrorType.class);
    when(error.getErrorType()).thenReturn(errorType);
    when(errorType.getParentErrorType()).thenReturn(CRITICAL_ERROR_TYPE);

    assertThat(handler.accept(event), is(true));
  }

}
