/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.MuleTestUtils.createErrorMock;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Test;

public class ExceptionTypeFilterTestCase extends AbstractMuleTestCase {

  @Test
  public void testExceptionTypeFilterMessage() {
    ExceptionTypeFilter filter = new ExceptionTypeFilter();
    assertThat(filter.getExpectedType(), nullValue());
    Message m = Message.of("test");
    assertThat(filter.accept(m, mock(Event.Builder.class)), is(false));

    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertThat(filter.accept(m, mock(Event.Builder.class)), is(true));

    filter = new ExceptionTypeFilter(IOException.class);
    assertThat(filter.accept(m, mock(Event.Builder.class)), is(false));
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertThat(filter.accept(m, mock(Event.Builder.class)), is(true));
  }

  @Test
  public void testExceptionTypeFilterEvent() {
    Event event = mock(Event.class);
    ExceptionTypeFilter filter = new ExceptionTypeFilter();
    assertThat(filter.getExpectedType(), nullValue());
    Message m = Message.of("test");
    assertThat(filter.accept(m, mock(Event.Builder.class)), is(false));

    Exception exception = new IllegalArgumentException("test");
    Error mockError = createErrorMock(exception);
    when(event.getError()).thenReturn(of(mockError));
    m = InternalMessage.builder(m).build();
    assertThat(filter.accept(event, mock(Event.Builder.class)), is(true));

    when(event.getMessage()).thenReturn(m);
    when(event.getError()).thenReturn(empty());
    filter = new ExceptionTypeFilter(IOException.class);
    assertThat(filter.accept(event, mock(Event.Builder.class)), is(false));
    exception = new IOException("test");
    mockError = createErrorMock(exception);
    when(event.getError()).thenReturn(of(mockError));
    assertThat(filter.accept(event, mock(Event.Builder.class)), is(true));
  }

}
