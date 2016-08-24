/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

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
    assertNull(filter.getExpectedType());
    MuleMessage m = MuleMessage.builder().payload("test").build();
    assertTrue(!filter.accept(m));

    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertTrue(filter.accept(m));

    filter = new ExceptionTypeFilter(IOException.class);
    assertTrue(!filter.accept(m));
    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertTrue(filter.accept(m));
  }

  @Test
  public void testExceptionTypeFilterEvent() {
    MuleEvent event = mock(MuleEvent.class);
    ExceptionTypeFilter filter = new ExceptionTypeFilter();
    assertNull(filter.getExpectedType());
    MuleMessage m = MuleMessage.builder().payload("test").build();
    assertTrue(!filter.accept(m));

    Exception exception = new IllegalArgumentException("test");
    when(event.getError()).thenReturn(new ErrorBuilder(exception).build());
    m = MuleMessage.builder(m).build();
    assertTrue(filter.accept(event));

    when(event.getError()).thenReturn(null);
    filter = new ExceptionTypeFilter(IOException.class);
    assertTrue(!filter.accept(event));
    exception = new IOException("test");
    when(event.getError()).thenReturn(new ErrorBuilder(exception).build());
    assertTrue(filter.accept(event));
  }

}
