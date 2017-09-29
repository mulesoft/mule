/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.ResponseCompletionCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.function.Consumer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionSourceExceptionCallbackTestCase extends AbstractMuleTestCase {

  private CoreEvent event;

  @Mock
  private MessageProcessContext messageProcessContext;

  @Mock
  private ResponseCompletionCallback responseCallback;

  @Mock
  private Consumer<MessagingException> exceptionHandlingCallback;

  @Mock
  private ErrorTypeLocator errorTypeLocator;

  @Mock
  private MessageSource messageSource;

  @Mock
  private ErrorType errorType;

  private ExtensionSourceExceptionCallback callback;
  private Exception exception = new Exception();

  @Before
  public void before() throws MuleException {
    event = newEvent();

    when(errorType.getIdentifier()).thenReturn("ID");
    when(errorType.getNamespace()).thenReturn("NS");
    when(errorTypeLocator.lookupErrorType(any(Exception.class))).thenReturn(errorType);
    when(messageProcessContext.getErrorTypeLocator()).thenReturn(errorTypeLocator);
    when(messageProcessContext.getMessageSource()).thenReturn(messageSource);
    callback = new ExtensionSourceExceptionCallback(responseCallback, event, exceptionHandlingCallback, messageProcessContext);
  }

  @Test
  public void onException() {
    callback.onException(exception);
    verify(responseCallback).responseSentWithFailure(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object o) {
        return o instanceof MessagingException && ((MessagingException) o).getRootCause().equals(exception);
      }
    }), argThat(new ArgumentMatcher<CoreEvent>() {

      @Override
      public boolean matches(Object o) {
        return o instanceof CoreEvent && ((CoreEvent) o).getError().isPresent()
            && ((CoreEvent) o).getError().get().getErrorType().equals(errorType);
      }
    }));
  }

  @Test
  public void exceptionHandlingCallbackInvoked() {
    ArgumentCaptor<MessagingException> exceptionCaptor = forClass(MessagingException.class);
    onException();

    verify(exceptionHandlingCallback).accept(exceptionCaptor.capture());

    MessagingException messagingException = exceptionCaptor.getValue();
    assertThat(messagingException, is(notNullValue()));
    assertThat(messagingException.getCause(), is(sameInstance(exception)));
  }
}
