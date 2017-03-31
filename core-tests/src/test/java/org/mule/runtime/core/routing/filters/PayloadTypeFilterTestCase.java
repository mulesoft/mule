/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class PayloadTypeFilterTestCase extends AbstractMuleTestCase {

  @Test
  public void testPayloadTypeFilterNoExpectedType() {
    PayloadTypeFilter filter = new PayloadTypeFilter();
    assertNull(filter.getExpectedType());
    assertFalse(filter.accept(of("test"), mock(Event.Builder.class)));

    filter.setExpectedType(String.class);
    assertTrue(filter.accept(of("test"), mock(Event.Builder.class)));

    filter.setExpectedType(null);
    assertFalse(filter.accept(of("test"), mock(Event.Builder.class)));
  }

  @Test
  public void testPayloadTypeFilter() {
    PayloadTypeFilter filter = new PayloadTypeFilter(Exception.class);
    assertNotNull(filter.getExpectedType());
    assertTrue(filter.accept(of((new Exception("test"))), mock(Event.Builder.class)));
    assertTrue(!filter.accept(of("test"), mock(Event.Builder.class)));

    filter.setExpectedType(String.class);
    assertTrue(filter.accept(of("test"), mock(Event.Builder.class)));
    assertTrue(!filter.accept(of(new Exception("test")), mock(Event.Builder.class)));
  }

}
