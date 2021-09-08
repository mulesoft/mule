/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.DataHandler;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.junit.Before;
import org.junit.Test;

public class MessagePropertiesTestCase extends AbstractELTestCase {

  private CoreEvent event;

  public MessagePropertiesTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Before
  public void setup() throws Exception {
    event = CoreEvent.builder(context).message(of("")).build();
  }

  @Test
  public void inboundPropertyMap() throws Exception {
    event = CoreEvent.builder(context)
        .message(InternalMessage.builder().value("").inboundProperties(singletonMap("foo", "bar")).build())
        .build();
    assertTrue(evaluate("message.inboundProperties", event) instanceof Map);
  }

  @Test
  public void assignToInboundPropertyMap() throws Exception {
    assertFinalProperty("message.inboundProperties='foo'", event);
  }

  @Test
  public void inboundProperty() throws Exception {
    event = CoreEvent.builder(context)
        .message(InternalMessage.builder().value("").inboundProperties(singletonMap("foo", "bar")).build())
        .build();
    assertEquals("bar", evaluate("message.inboundProperties['foo']", event));
  }

  @Test
  public void assignValueToInboundProperty() throws Exception {
    event = CoreEvent.builder(context)
        .message(InternalMessage.builder().value("").inboundProperties(singletonMap("foo", "bar")).build())
        .build();
    assertUnsupportedOperation("message.inboundProperties['foo']='bar'", event);
  }

  @Test
  public void assignValueToNewInboundProperty() throws Exception {
    assertUnsupportedOperation("message.inboundProperties['foo_new']='bar'", event);
  }

  @Test
  public void outboundPropertyMap() throws Exception {
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "bar").build())
        .build();
    assertTrue(evaluate("message.outboundProperties", event) instanceof Map);
  }

  @Test
  public void assignToOutboundPropertyMap() throws Exception {
    assertFinalProperty("message.outboundProperties='foo'", event);
  }

  @Test
  public void outboundProperty() throws Exception {
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "bar").build())
        .build();
    assertEquals("bar", evaluate("message.outboundProperties['foo']", event));
  }

  @Test
  public void assignValueToOutboundProperty() throws Exception {
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "bar_old").build()).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    evaluate("message.outboundProperties['foo']='bar'", event, eventBuilder);

    assertEquals("bar", ((InternalMessage) eventBuilder.build().getMessage()).getOutboundProperty("foo"));
  }

  @Test
  public void assignValueToNewOutboundProperty() throws Exception {
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    evaluate("message.outboundProperties['foo']='bar'", event, eventBuilder);
    assertEquals("bar", ((InternalMessage) eventBuilder.build().getMessage()).getOutboundProperty("foo"));
  }

  @Test
  public void inboundClear() throws Exception {
    assertUnsupportedOperation("message.inboundProperties.clear())", event);
  }

  @Test
  public void inboundSize() throws Exception {
    Message message = event.getMessage();
    mock(DataHandler.class);
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(message).addInboundProperty("foo", "abc").addInboundProperty("bar", "xyz").build())
        .build();
    assertEquals(2, evaluate("message.inboundProperties.size()", event));
  }

  @Test
  public void inboundKeySet() throws Exception {
    Message message = event.getMessage();
    mock(DataHandler.class);
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(message).addInboundProperty("foo", "abc").addInboundProperty("bar", "xyz").build())
        .build();
    assertThat((Iterable<String>) evaluate("message.inboundProperties.keySet()", event), hasItems("foo", "bar"));
  }

  @Test
  public void inboundContainsKey() throws Exception {
    Message message = event.getMessage();
    mock(DataHandler.class);
    event =
        CoreEvent.builder(event).message(InternalMessage.builder(message).addInboundProperty("foo", "abc").build()).build();
    assertTrue((Boolean) evaluate("message.inboundProperties.containsKey('foo')", event));
    assertFalse((Boolean) evaluate("message.inboundProperties.containsKey('bar')", event));
  }

  @Test
  public void inboundContainsValue() throws Exception {
    Message message = event.getMessage();
    event =
        CoreEvent.builder(event).message(InternalMessage.builder(message).addInboundProperty("foo", "abc").build()).build();
    assertTrue((Boolean) evaluate("message.inboundProperties.containsValue('abc')", event));
    assertFalse((Boolean) evaluate("message.inboundProperties.containsValue('xyz')", event));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void inboundEntrySet() throws Exception {
    Message message = event.getMessage();
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(message).addInboundProperty("foo", "abc").addInboundProperty("bar", "xyz").build())
        .build();
    Set<Map.Entry<String, Object>> entrySet =
        (Set<Entry<String, Object>>) evaluate("message.inboundProperties.entrySet()", event);
    assertEquals(2, entrySet.size());
    entrySet.contains(new DefaultMapEntry("foo", "abc"));
    entrySet.contains(new DefaultMapEntry("bar", "xyz"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void inboundValues() throws Exception {
    Message message = event.getMessage();
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(message).addInboundProperty("foo", "abc").addInboundProperty("bar", "xyz").build())
        .build();
    Collection<DataHandler> values = (Collection<DataHandler>) evaluate("message.inboundProperties.values()", event);
    assertEquals(2, values.size());
    values.contains("abc");
    values.contains("xyz");
  }

  @Test
  public void inboundIsEmpty() throws Exception {
    Message message = event.getMessage();
    assertTrue((Boolean) evaluate("message.inboundProperties.isEmpty()", event));
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(message).addInboundProperty("foo", "abc").addInboundProperty("bar", "xyz").build())
        .build();
    assertFalse((Boolean) evaluate("message.inboundProperties.isEmpty()", event));
  }

  @Test
  public void inboundPutAll() throws Exception {
    assertUnsupportedOperation("message.inboundProperties.putAll(['foo': 'abc','bar': 'xyz'])", event);
  }

  @Test
  public void inboundRemove() throws Exception {
    assertUnsupportedOperation("message.inboundProperties.remove('foo')", event);
  }

  @Test
  public void outboundClear() throws Exception {
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc")
        .addOutboundProperty("bar", "xyz").build()).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    evaluate("message.outboundProperties.clear()", event, eventBuilder);
    assertEquals(0, ((InternalMessage) eventBuilder.build().getMessage()).getOutboundPropertyNames().size());
  }

  @Test
  public void outboundSize() throws Exception {
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc")
        .addOutboundProperty("bar", "xyz").build()).build();
    assertEquals(2, evaluate("message.outboundProperties.size()", event));
  }

  @Test
  public void outboundKeySet() throws Exception {
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc")
        .addOutboundProperty("bar", "xyz").build()).build();
    assertThat(evaluate("message.outboundProperties.keySet().toArray()[0]", event), anyOf(equalTo("foo"), equalTo("bar")));
    assertThat(evaluate("message.outboundProperties.keySet().toArray()[1]", event), anyOf(equalTo("foo"), equalTo("bar")));
  }

  @Test
  public void outboundContainsKey() throws Exception {
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc").build())
        .build();
    assertTrue((Boolean) evaluate("message.outboundProperties.containsKey('foo')", event));
    assertFalse((Boolean) evaluate("message.outboundProperties.containsKey('bar')", event));
  }

  @Test
  public void outboundContainsValue() throws Exception {
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc").build())
        .build();
    assertTrue((Boolean) evaluate("message.outboundProperties.containsValue('abc')", event));
    assertFalse((Boolean) evaluate("message.outboundProperties.containsValue('xyz')", event));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void outboundEntrySet() throws Exception {
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc")
        .addOutboundProperty("bar", "xyz").build()).build();
    Set<Map.Entry<String, Object>> entrySet =
        (Set<Entry<String, Object>>) evaluate("message.outboundProperties.entrySet()", event);
    assertEquals(2, entrySet.size());
    entrySet.contains(new DefaultMapEntry("foo", "abc"));
    entrySet.contains(new DefaultMapEntry("bar", "xyz"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void outboundValues() throws Exception {
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc")
        .addOutboundProperty("bar", "xyz").build()).build();
    Collection<DataHandler> values = (Collection<DataHandler>) evaluate("message.outboundProperties.values()", event);
    assertEquals(2, values.size());
    values.contains("abc");
    values.contains("xyz");
  }

  @Test
  public void outboundIsEmpty() throws Exception {
    assertTrue((Boolean) evaluate("message.outboundProperties.isEmpty()", event));
    event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc")
        .addOutboundProperty("bar", "xyz").build()).build();
    assertFalse((Boolean) evaluate("message.outboundProperties.isEmpty()", event));
  }

  @Test
  public void outboundPutAll() throws Exception {
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    evaluate("message.outboundProperties.putAll(['foo': 'abc','bar': 'xyz'])", event, eventBuilder);
    assertEquals("abc", evaluate("message.outboundProperties['foo']", eventBuilder.build()));
    assertEquals("xyz", evaluate("message.outboundProperties['bar']", eventBuilder.build()));
  }

  @Test
  public void outboundInboundRemove() throws Exception {
    event = CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("foo", "abc").build())
        .build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    assertFalse((Boolean) evaluate("message.outboundProperties.isEmpty()", event));
    evaluate("message.outboundProperties.remove('foo')", event, eventBuilder);
    assertTrue((Boolean) evaluate("message.outboundProperties.isEmpty()", eventBuilder.build()));
  }

}
