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

import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class PayloadTypeFilterTestCase extends AbstractMuleTestCase {

  @Test
  public void testPayloadTypeFilterNoExpectedType() {
    PayloadTypeFilter filter = new PayloadTypeFilter();
    assertNull(filter.getExpectedType());
    assertFalse(filter.accept(MuleMessage.builder().payload("test").build()));

    filter.setExpectedType(String.class);
    assertTrue(filter.accept(MuleMessage.builder().payload("test").build()));

    filter.setExpectedType(null);
    assertFalse(filter.accept(MuleMessage.builder().payload("test").build()));
  }

  @Test
  public void testPayloadTypeFilter() {
    PayloadTypeFilter filter = new PayloadTypeFilter(Exception.class);
    assertNotNull(filter.getExpectedType());
    assertTrue(filter.accept(MuleMessage.builder().payload(new Exception("test")).build()));
    assertTrue(!filter.accept(MuleMessage.builder().payload("test").build()));

    filter.setExpectedType(String.class);
    assertTrue(filter.accept(MuleMessage.builder().payload("test").build()));
    assertTrue(!filter.accept(MuleMessage.builder().payload(new Exception("test")).build()));
  }

}
