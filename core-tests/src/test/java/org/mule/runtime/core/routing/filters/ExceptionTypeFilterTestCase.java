/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.message.ErrorBuilder.builder;

import java.io.IOException;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class ExceptionTypeFilterTestCase extends AbstractMuleTestCase {

  @Test
  public void testExceptionTypeFilterMessage() {
    ExceptionTypeFilter filter = new ExceptionTypeFilter();
    assertThat(filter.getExpectedType(), nullValue());
    MuleMessage m = MuleMessage.builder().payload("test").build();
    assertThat(filter.accept(m), is(false));

    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertThat(filter.accept(m), is(true));

    filter = new ExceptionTypeFilter(IOException.class);
    assertThat(filter.accept(m), is(false));
    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertThat(filter.accept(m), is(true));
  }

  @Test
  public void testExceptionTypeFilterEvent() {
    MuleEvent event = mock(MuleEvent.class);
    ExceptionTypeFilter filter = new ExceptionTypeFilter();
    assertThat(filter.getExpectedType(), nullValue());
    MuleMessage m = MuleMessage.builder().payload("test").build();
    assertThat(filter.accept(m), is(false));

    Exception exception = new IllegalArgumentException("test");
    when(event.getError()).thenReturn(builder(exception).build());
    m = MuleMessage.builder(m).build();
    assertThat(filter.accept(event), is(true));

    when(event.getMessage()).thenReturn(m);
    when(event.getError()).thenReturn(null);
    filter = new ExceptionTypeFilter(IOException.class);
    assertThat(filter.accept(event), is(false));
    exception = new IOException("test");
    when(event.getError()).thenReturn(builder(exception).build());
    assertThat(filter.accept(event), is(true));
  }

}
