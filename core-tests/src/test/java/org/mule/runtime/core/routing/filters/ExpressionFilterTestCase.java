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
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class ExpressionFilterTestCase extends AbstractMuleContextTestCase {

  private Builder eventBuilder;

  @Before
  public void before() throws MuleException {
    eventBuilder = Event.builder(DefaultEventContext.create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION));
  }

  @Test
  public void testHeaderFilterEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:message.outboundProperties['foo']=='bar'");
    filter.setMuleContext(muleContext);
    Message message = of("blah");
    assertTrue(!filter.accept(message, eventBuilder));

    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue(filter.accept(message, eventBuilder));
  }

  @Test
  public void testVariableFilterEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:flowVars['foo']=='bar'");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(testEvent(), eventBuilder));
    Event event = Event.builder(testEvent()).addVariable("foo", "bar").build();
    assertTrue(filter.accept(event, eventBuilder));
  }

  @Test
  public void testHeaderFilterWithNotEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:message.outboundProperties['foo']!='bar'");
    filter.setMuleContext(muleContext);

    Message message = of("blah");

    assertTrue(filter.accept(message, eventBuilder));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue(!filter.accept(message, eventBuilder));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue(filter.accept(message, eventBuilder));
  }

  @Test
  public void testVariableFilterWithNotEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:flowVars['foo']!='bar'");
    filter.setMuleContext(muleContext);

    assertTrue(filter.accept(testEvent(), eventBuilder));
    Event event = Event.builder(testEvent()).addVariable("foo", "bar").build();
    assertTrue(!filter.accept(event, eventBuilder));
    event = Event.builder(event).addVariable("foo", "car").build();
    assertTrue(filter.accept(event, eventBuilder));
  }

  private Message removeProperty(Message message) {
    return InternalMessage.builder(message).removeOutboundProperty("foo").build();
  }

  @Test
  public void testHeaderFilterWithNotNullEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:message.outboundProperties['foo']!=null");
    filter.setMuleContext(muleContext);

    Message message = of("blah");

    assertTrue(!filter.accept(message, eventBuilder));
    message = removeProperty(message);
    assertTrue(!filter.accept(message, eventBuilder));
    message = InternalMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue(filter.accept(message, eventBuilder));
  }

  @Test
  public void testVariableFilterWithNotNullEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("mel:flowVars['foo']!=null");
    filter.setMuleContext(muleContext);

    assertTrue(!filter.accept(testEvent(), eventBuilder));
    Event event = Event.builder(testEvent()).message(removeProperty(testEvent().getMessage())).build();
    assertTrue(!filter.accept(event, eventBuilder));
    event = Event.builder(event).addVariable("foo", "car").build();
    assertTrue(filter.accept(event, eventBuilder));
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

    assertTrue(filter.accept(of("The number is 4"), eventBuilder));
    assertFalse(filter.accept(of("Say again?"), eventBuilder));
    assertFalse(filter.accept(of("The number is 0"), eventBuilder));
  }

  @Test
  public void testExceptionTypeFilter() {
    ExpressionFilter filter = new ExpressionFilter("mel:exception.getCause() is java.lang.Exception");
    filter.setMuleContext(muleContext);

    Message m = of("test");
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertTrue(filter.accept(m, eventBuilder));

    filter = new ExpressionFilter("mel:exception.getCause() is java.io.IOException");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(m, eventBuilder));
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertTrue(filter.accept(m, eventBuilder));
  }

  @Test
  public void testExceptionTypeFilterEL() {
    ExpressionFilter filter = new ExpressionFilter("mel:exception.getCause() is java.lang.Exception");
    filter.setMuleContext(muleContext);

    Message m = of("test");
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertTrue(filter.accept(m, eventBuilder));

    filter = new ExpressionFilter("mel:exception.getCause() is java.io.IOException");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(m, eventBuilder));
    m = InternalMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertTrue(filter.accept(m, eventBuilder));
  }

  @Test
  public void testPayloadTypeFilterEL() {
    ExpressionFilter filter = new ExpressionFilter("mel:payload is org.mule.tck.testmodels.fruit.Apple");
    filter.setMuleContext(muleContext);

    assertTrue(filter.accept(of(new Apple()), eventBuilder));
    assertTrue(!filter.accept(of("test"), eventBuilder));

    filter = new ExpressionFilter("mel:payload is String");
    filter.setMuleContext(muleContext);
    assertTrue(filter.accept(of("test"), eventBuilder));
    assertTrue(!filter.accept(of(new Exception("test")), eventBuilder));
  }

  @Test
  public void testTrueStringEL() {
    ExpressionFilter filter = new ExpressionFilter("payload");
    filter.setMuleContext(muleContext);

    filter.setNullReturnsTrue(true);

    assertTrue(filter.accept(of("true"), eventBuilder));
    assertTrue(filter.accept(of("TRUE"), eventBuilder));
    assertTrue(filter.accept(of("tRuE"), eventBuilder));
  }

  @Test
  public void testFalseStringEL() {
    ExpressionFilter filter = new ExpressionFilter("payload");
    filter.setMuleContext(muleContext);

    filter.setNullReturnsTrue(false);

    assertFalse(filter.accept(of("false"), eventBuilder));
    assertFalse(filter.accept(of("FALSE"), eventBuilder));
    assertFalse(filter.accept(of("faLSe"), eventBuilder));
  }

}
