/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;

import org.junit.Test;

public class ExceptionTestCase extends AbstractELTestCase {

  private Error mockError = mock(Error.class);

  public ExceptionTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Override
  public void setupFlowConstruct() throws Exception {
    flowConstruct = getTestFlow(muleContext);
  }

  @Test
  public void exception() throws Exception {
    Event event = createEvent();
    RuntimeException rte = new RuntimeException();
    when(mockError.getCause()).thenReturn(rte);
    event = Event.builder(event).message(InternalMessage.builder(event.getMessage()).build()).build();
    Object exception = evaluate("exception", event);

    assertThat(exception, is(instanceOf(MessagingException.class)));
    assertThat(((MessagingException) exception).getCause(), is(rte));
  }

  @Test
  public void assignException() throws Exception {
    Event event = createEvent();
    event = Event.builder(event).message(InternalMessage.builder(event.getMessage()).build()).build();
    RuntimeException runtimeException = new RuntimeException();
    when(mockError.getCause()).thenReturn(runtimeException);
    assertImmutableVariable("exception='other'", event);
  }

  @Test
  public void exceptionCausedBy() throws Exception {
    Event event = createEvent();
    Message message = event.getMessage();
    MessagingException me =
        new MessagingException(CoreMessages.createStaticMessage(""),
                               Event.builder(context).message(message).flow(flowConstruct).build(),
                               new IllegalAccessException());
    when(mockError.getCause()).thenReturn(me);
    assertTrue((Boolean) evaluate("exception.causedBy(java.lang.IllegalAccessException)", event));
  }

  private Event createEvent() throws Exception {
    return Event.builder(context).message(of("")).flow(flowConstruct).error(mockError).build();
  }
}
