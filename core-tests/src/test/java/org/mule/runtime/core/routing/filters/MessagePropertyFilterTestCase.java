/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MessagePropertyFilterTestCase extends AbstractMuleTestCase {

  @Test
  public void testMessagePropertyFilter() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=bar");
    InternalMessage message = InternalMessage.builder().payload("blah").build();
    assertTrue(!filter.accept(message, mock(Event.Builder.class)));

    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyFilterInboundScope() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("blah").build();
    MessagePropertyFilter filter = new MessagePropertyFilter("inbound:foo=bar");
    assertEquals("inbound", filter.getScope());

    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addInboundProperty("foo", "bar").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyFilterWithURL() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("blah").build();
    MessagePropertyFilter filter = new MessagePropertyFilter("inbound:foo=http://foo.com");
    assertEquals("inbound", filter.getScope());

    assertFalse(filter.accept(message, mock(Event.Builder.class)));

    Map inboundProps = new HashMap();
    inboundProps.put("foo", "http://foo.com");
    message = InternalMessage.builder().payload("blah").inboundProperties(inboundProps).build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));

    // Checking here that a ':' in the value doesn't throw things off
    filter = new MessagePropertyFilter("bar=http://bar.com");
    // default scope
    assertEquals("outbound", filter.getScope());

    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("bar", "http://bar.com").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyFilterWithNot() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo!=bar");
    InternalMessage message = InternalMessage.builder().payload("blah").build();

    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyFilterWithNotNull() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo!=null");
    InternalMessage message = InternalMessage.builder().payload("blah").build();

    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    message = removeProperty(message, "foo");
    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyFilterWithCaseSensitivity() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=Bar");
    InternalMessage message = InternalMessage.builder().payload("blah").build();
    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    filter.setCaseSensitive(false);
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyFilterWithWildcard() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=B*");
    InternalMessage message = InternalMessage.builder().payload("blah").build();
    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    filter.setCaseSensitive(false);
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
    filter.setPattern("foo=*a*");
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyFilterDodgyValues() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter();
    assertFalse(filter.accept((InternalMessage) null, mock(Event.Builder.class)));

    filter = new MessagePropertyFilter("foo = bar");
    InternalMessage message = InternalMessage.builder().payload("blah").build();
    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
    filter.setCaseSensitive(false);

    filter = new MessagePropertyFilter("foo2 =null");
    message = removeProperty(message, "foo2");
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));

    filter = new MessagePropertyFilter("foo2 =");
    message = InternalMessage.builder(message).addOutboundProperty("foo2", "").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));

    message = removeProperty(message, "foo2");
    assertFalse(filter.accept(message, mock(Event.Builder.class)));
  }

  private InternalMessage removeProperty(InternalMessage message, String property) {
    return InternalMessage.builder(message).removeOutboundProperty(property).build();
  }

  @Test
  public void testMessagePropertyFilterPropertyExists() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo!=null");
    InternalMessage message = InternalMessage.builder().payload("blah").build();

    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue("Filter didn't accept the message", filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testMessagePropertyWithEnum() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=ONE_WAY");
    InternalMessage message = InternalMessage.builder().payload("").build();
    assertFalse(filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", ONE_WAY).build();
    assertTrue(filter.accept(message, mock(Event.Builder.class)));
  }
}
