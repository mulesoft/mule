/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.junit.Test;

public class ExceptionTestCase extends AbstractELTestCase {

  private Error mockError = mock(Error.class);
  private ErrorType mockErrorType = mock(ErrorType.class);

  public ExceptionTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Override
  public void setupFlowConstruct() throws Exception {
    flowConstruct = getTestFlow(muleContext);
  }

  @Test
  public void exception() throws Exception {
    CoreEvent event = createEvent();
    RuntimeException rte = new RuntimeException();
    when(mockError.getCause()).thenReturn(rte);
    when(mockError.getErrorType()).thenReturn(mockErrorType);
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).build()).build();
    Object exception = evaluate("exception", event);

    assertThat(exception, is(instanceOf(MessagingException.class)));
    assertThat(((MessagingException) exception).getCause(), is(rte));
  }

  @Test
  public void assignException() throws Exception {
    CoreEvent event = createEvent();
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).build()).build();
    RuntimeException runtimeException = new RuntimeException();
    when(mockError.getCause()).thenReturn(runtimeException);
    when(mockError.getErrorType()).thenReturn(mockErrorType);
    assertImmutableVariable("exception='other'", event);
  }

  @Test
  public void exceptionCausedBy() throws Exception {
    CoreEvent event = createEvent();
    Message message = event.getMessage();
    MessagingException me =
        new MessagingException(CoreMessages.createStaticMessage(""),
                               InternalEvent.builder(context).message(message).build(),
                               new IllegalAccessException());
    when(mockError.getCause()).thenReturn(me);
    assertTrue((Boolean) evaluate("exception.causedBy(java.lang.IllegalAccessException)", event));
  }

  private PrivilegedEvent createEvent() throws Exception {
    return PrivilegedEvent.builder(context).message(of("")).error(mockError).build();
  }
}
