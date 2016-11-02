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
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionSourceExceptionCallbackTestCase extends AbstractMuleTestCase {

  @Mock
  private Event event;

  @Mock
  private ResponseCompletionCallback responseCallback;

  @Mock
  private Consumer<MessagingException> exceptionHandlingCallback;

  private ExtensionSourceExceptionCallback callback;
  private Exception exception = new Exception();

  @Before
  public void before() {
    callback = new ExtensionSourceExceptionCallback(responseCallback, event, exceptionHandlingCallback);
  }

  @Test
  public void onException() {
    callback.onException(exception);
    verify(responseCallback).responseSentWithFailure(argThat(new ArgumentMatcher<MessagingException>() {

      @Override
      public boolean matches(Object o) {
        return o instanceof MessagingException && ((MessagingException) o).getCauseException().equals(exception);
      }
    }), eq(event));
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
