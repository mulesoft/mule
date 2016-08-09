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
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MessagePropertyFilterTestCase extends AbstractMuleTestCase {

  @Test
  public void testMessagePropertyFilter() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=bar");
    MuleMessage message = MuleMessage.builder().payload("blah").build();
    assertTrue(!filter.accept(message));

    message = MuleMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyFilterInboundScope() throws Exception {
    MuleMessage message = MuleMessage.builder().payload("blah").build();
    MessagePropertyFilter filter = new MessagePropertyFilter("inbound:foo=bar");
    assertEquals("inbound", filter.getScope());

    assertFalse(filter.accept(message));
    message = MuleMessage.builder(message).addInboundProperty("foo", "bar").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyFilterWithURL() throws Exception {
    MuleMessage message = MuleMessage.builder().payload("blah").build();
    MessagePropertyFilter filter = new MessagePropertyFilter("inbound:foo=http://foo.com");
    assertEquals("inbound", filter.getScope());

    assertFalse(filter.accept(message));

    Map inboundProps = new HashMap();
    inboundProps.put("foo", "http://foo.com");
    message = MuleMessage.builder().payload("blah").inboundProperties(inboundProps).build();
    assertTrue("Filter didn't accept the message", filter.accept(message));

    // Checking here that a ':' in the value doesn't throw things off
    filter = new MessagePropertyFilter("bar=http://bar.com");
    // default scope
    assertEquals("outbound", filter.getScope());

    assertFalse(filter.accept(message));
    message = MuleMessage.builder(message).addOutboundProperty("bar", "http://bar.com").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyFilterWithNot() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo!=bar");
    MuleMessage message = MuleMessage.builder().payload("blah").build();

    assertTrue("Filter didn't accept the message", filter.accept(message));
    message = MuleMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertFalse(filter.accept(message));
    message = MuleMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyFilterWithNotNull() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo!=null");
    MuleMessage message = MuleMessage.builder().payload("blah").build();

    assertFalse(filter.accept(message));
    message = removeProperty(message, "foo");
    assertFalse(filter.accept(message));
    message = MuleMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyFilterWithCaseSensitivity() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=Bar");
    MuleMessage message = MuleMessage.builder().payload("blah").build();
    message = MuleMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertFalse(filter.accept(message));
    filter.setCaseSensitive(false);
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyFilterWithWildcard() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=B*");
    MuleMessage message = MuleMessage.builder().payload("blah").build();
    message = MuleMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertFalse(filter.accept(message));
    filter.setCaseSensitive(false);
    assertTrue("Filter didn't accept the message", filter.accept(message));
    filter.setPattern("foo=*a*");
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyFilterDodgyValues() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter();
    assertFalse(filter.accept((MuleMessage) null));

    filter = new MessagePropertyFilter("foo = bar");
    MuleMessage message = MuleMessage.builder().payload("blah").build();
    message = MuleMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));
    filter.setCaseSensitive(false);

    filter = new MessagePropertyFilter("foo2 =null");
    message = removeProperty(message, "foo2");
    assertTrue("Filter didn't accept the message", filter.accept(message));

    filter = new MessagePropertyFilter("foo2 =");
    message = MuleMessage.builder(message).addOutboundProperty("foo2", "").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));

    message = removeProperty(message, "foo2");
    assertFalse(filter.accept(message));
  }

  private MuleMessage removeProperty(MuleMessage message, String property) {
    return MuleMessage.builder(message).removeOutboundProperty(property).build();
  }

  @Test
  public void testMessagePropertyFilterPropertyExists() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo!=null");
    MuleMessage message = MuleMessage.builder().payload("blah").build();

    assertFalse(filter.accept(message));
    message = MuleMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue("Filter didn't accept the message", filter.accept(message));
  }

  @Test
  public void testMessagePropertyWithEnum() throws Exception {
    MessagePropertyFilter filter = new MessagePropertyFilter("foo=ONE_WAY");
    MuleMessage message = MuleMessage.builder().payload("").build();
    assertFalse(filter.accept(message));
    message = MuleMessage.builder(message).addOutboundProperty("foo", ONE_WAY).build();
    assertTrue(filter.accept(message));
  }
}
