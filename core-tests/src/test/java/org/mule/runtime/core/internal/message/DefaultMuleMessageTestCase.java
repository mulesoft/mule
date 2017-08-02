/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DefaultMuleMessageTestCase extends AbstractMuleContextTestCase {

  public static final String FOO_PROPERTY = "foo";

  @Test
  public void testMessagePropertiesAccessors() {
    Map<String, Serializable> properties = createMessageProperties();

    properties.put("number", "24");
    properties.put("decimal", "24.3");
    properties.put("boolean", "true");
    Apple apple = new Apple(true);
    properties.put("apple", apple);
    InternalMessage message =
        (InternalMessage) InternalMessage.builder().value(TEST_MESSAGE).outboundProperties(properties).build();
    assertTrue(message.getOutboundProperty("boolean", false));
    assertEquals(new Integer(24), message.getOutboundProperty("number", 0));
    assertEquals(new Byte((byte) 24), message.getOutboundProperty("number", (byte) 0));
    assertEquals(new Long(24), message.getOutboundProperty("number", 0l));
    assertEquals(new Float(24.3), message.getOutboundProperty("decimal", 0f));
    Double d = message.getOutboundProperty("decimal", 0d);
    assertEquals(new Double(24.3), d);

    assertEquals("true", message.getOutboundProperty("boolean", ""));

    assertEquals(apple, message.getOutboundProperty("apple"));
    try {
      message.getOutboundProperty("apple", new Orange());
      fail("Orange is not assignable to Apple");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // Test null
    assertNull(message.getOutboundProperty("banana"));
    assertNull(message.getOutboundProperty("blah"));

    // Test default value
    assertEquals(new Float(24.3), message.getOutboundProperty("blah", 24.3f));

  }

  @Test
  public void testClearProperties() {
    InternalMessage payload =
        (InternalMessage) InternalMessage.builder(createMuleMessage()).addOutboundProperty(FOO_PROPERTY, "fooValue").build();

    assertThat(payload.getOutboundPropertyNames(), hasSize(2));
    assertThat(payload.getInboundPropertyNames(), empty());

    payload = (InternalMessage) InternalMessage.builder(payload).outboundProperties(emptyMap()).build();
    assertThat(payload.getOutboundPropertyNames(), empty());

    // See http://www.mulesoft.org/jira/browse/MULE-4968 for additional test needed here
  }

  //
  // helpers
  //
  private Map<String, Serializable> createMessageProperties() {
    HashMap<String, Serializable> map = new HashMap<>();
    map.put("MessageProperties", "MessageProperties");
    return map;
  }

  private Message createMuleMessage() {
    return InternalMessage.builder().value(TEST_PAYLOAD)
        .addOutboundProperty("Message", "Message").build();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testPropertyNamesImmutable() throws Exception {
    InternalMessage message = (InternalMessage) createMuleMessage();
    message.getOutboundPropertyNames().add("other");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testInboundPropertyNamesAddImmutable() throws Exception {
    InternalMessage message = (InternalMessage) createMuleMessage();
    message.getOutboundPropertyNames().add("other");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testOutboundPropertyNamesImmutable() throws Exception {
    Message message = createMuleMessage();
    ((InternalMessage) message).getOutboundPropertyNames().add("other");
  }

  @Test
  public void usesNullPayloadAsNull() throws Exception {
    Message message = InternalMessage.builder(createMuleMessage()).addOutboundProperty(FOO_PROPERTY, null).build();

    assertThat(((InternalMessage) message).getOutboundProperty(FOO_PROPERTY), is(nullValue()));
  }

}
