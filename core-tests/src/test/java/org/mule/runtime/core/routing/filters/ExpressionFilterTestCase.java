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

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.IOException;

import org.junit.Test;

public class ExpressionFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testHeaderFilterEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:message.outboundProperties['foo']=='bar'");
    filter.setMuleContext(muleContext);
    InternalMessage message = InternalMessage.builder().payload("blah").build();
    assertTrue(!filter.accept(message, mock(Event.Builder.class)));

    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue(filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testVariableFilterEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:flowVars['foo']=='bar'");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(testEvent(), mock(Event.Builder.class)));
    Event event = Event.builder(testEvent()).addVariable("foo", "bar").build();
    assertTrue(filter.accept(event, mock(Event.Builder.class)));
  }

  @Test
  public void testHeaderFilterWithNotEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:message.outboundProperties['foo']!='bar'");
    filter.setMuleContext(muleContext);

    InternalMessage message = InternalMessage.builder().payload("blah").build();

    assertTrue(filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue(!filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue(filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testVariableFilterWithNotEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:flowVars['foo']!='bar'");
    filter.setMuleContext(muleContext);

    assertTrue(filter.accept(testEvent(), mock(Event.Builder.class)));
    Event event = Event.builder(testEvent()).addVariable("foo", "bar").build();
    assertTrue(!filter.accept(event, mock(Event.Builder.class)));
    event = Event.builder(event).addVariable("foo", "car").build();
    assertTrue(filter.accept(event, mock(Event.Builder.class)));
  }

  private InternalMessage removeProperty(InternalMessage message) {
    return InternalMessage.builder(message).removeOutboundProperty("foo").build();
  }

  @Test
  public void testHeaderFilterWithNotNullEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:message.outboundProperties['foo']!=null");
    filter.setMuleContext(muleContext);

    InternalMessage message = InternalMessage.builder().payload("blah").build();

    assertTrue(!filter.accept(message, mock(Event.Builder.class)));
    message = removeProperty(message);
    assertTrue(!filter.accept(message, mock(Event.Builder.class)));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue(filter.accept(message, mock(Event.Builder.class)));
  }

  @Test
  public void testVariableFilterWithNotNullEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:flowVars['foo']!=null");
    filter.setMuleContext(muleContext);

    assertTrue(!filter.accept(testEvent(), mock(Event.Builder.class)));
    Event event = Event.builder(testEvent()).message(removeProperty(testEvent().getMessage())).build();
    assertTrue(!filter.accept(event, mock(Event.Builder.class)));
    event = Event.builder(event).addVariable("foo", "car").build();
    assertTrue(filter.accept(event, mock(Event.Builder.class)));
  }

  @Test
  public void testRegexFilterNoPattern() {
    // start with default
    RegExFilter filter = new RegExFilter();
    assertNull(filter.getPattern());
    assertFalse(filter.accept("No tengo dinero"));

    // activate a pattern
    filter.setPattern("(.*) brown fox");
    assertTrue(filter.accept("The quick brown fox"));

    // remove pattern again, i.e. block all
    filter.setPattern(null);
    assertFalse(filter.accept("oh-oh"));
  }

  @Test
  public void testRegexFilterWithAngleBrackets() {

    ExpressionFilter filter = new ExpressionFilter("#[mel:regex('The number is [1-9]')]");
    filter.setMuleContext(muleContext);

    assertNotNull(filter.getExpression());

    assertTrue(filter.accept(InternalMessage.builder().payload("The number is 4").build(), mock(Event.Builder.class)));
    assertFalse(filter.accept(InternalMessage.builder().payload("Say again?").build(), mock(Event.Builder.class)));

    assertFalse(filter.accept(InternalMessage.builder().payload("The number is 0").build(), mock(Event.Builder.class)));
  }

  @Test
  public void testExceptionTypeFilter() {
    ExpressionFilter filter = new ExpressionFilter("mel:exception.getCause() is java.lang.Exception");
    filter.setMuleContext(muleContext);

    InternalMessage m = InternalMessage.builder().payload("test").build();
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertTrue(filter.accept(m, mock(Event.Builder.class)));

    filter = new ExpressionFilter("mel:exception.getCause() is java.io.IOException");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(m, mock(Event.Builder.class)));
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertTrue(filter.accept(m, mock(Event.Builder.class)));
  }

  @Test
  public void testExceptionTypeFilterEL() {
    ExpressionFilter filter = new ExpressionFilter("mel:exception.getCause() is java.lang.Exception");
    filter.setMuleContext(muleContext);

    InternalMessage m = InternalMessage.builder().payload("test").build();
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertTrue(filter.accept(m, mock(Event.Builder.class)));

    filter = new ExpressionFilter("mel:exception.getCause() is java.io.IOException");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(m, mock(Event.Builder.class)));
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertTrue(filter.accept(m, mock(Event.Builder.class)));
  }

  @Test
  public void testPayloadTypeFilterEL() {
    ExpressionFilter filter = new ExpressionFilter("mel:payload is org.mule.tck.testmodels.fruit.Apple");
    filter.setMuleContext(muleContext);

    assertTrue(filter.accept(InternalMessage.builder().payload(new Apple()).build(), mock(Event.Builder.class)));
    assertTrue(!filter.accept(InternalMessage.builder().payload("test").build(), mock(Event.Builder.class)));

    filter = new ExpressionFilter("mel:payload is String");
    filter.setMuleContext(muleContext);
    assertTrue(filter.accept(InternalMessage.builder().payload("test").build(), mock(Event.Builder.class)));
    assertTrue(!filter.accept(InternalMessage.builder().payload(new Exception("test")).build(), mock(Event.Builder.class)));
  }

  @Test
  public void testTrueStringEL() {
    ExpressionFilter filter = new ExpressionFilter("payload");
    filter.setMuleContext(muleContext);

    filter.setNullReturnsTrue(true);

    assertTrue(filter.accept(InternalMessage.builder().payload("true").build(), mock(Event.Builder.class)));
    assertTrue(filter.accept(InternalMessage.builder().payload("TRUE").build(), mock(Event.Builder.class)));
    assertTrue(filter.accept(InternalMessage.builder().payload("tRuE").build(), mock(Event.Builder.class)));
  }

  @Test
  public void testFalseStringEL() {
    ExpressionFilter filter = new ExpressionFilter("payload");
    filter.setMuleContext(muleContext);

    filter.setNullReturnsTrue(false);

    assertFalse(filter.accept(InternalMessage.builder().payload("false").build(), mock(Event.Builder.class)));
    assertFalse(filter.accept(InternalMessage.builder().payload("FALSE").build(), mock(Event.Builder.class)));
    assertFalse(filter.accept(InternalMessage.builder().payload("faLSe").build(), mock(Event.Builder.class)));
  }

}
