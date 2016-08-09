/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Test;

public class ExceptionTypeFilterTestCase extends AbstractMuleTestCase {

  @Test
  public void testExceptionTypeFilter() {
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

}
