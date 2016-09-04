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

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.IOException;

import org.junit.Test;

public class ExpressionFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testHeaderFilterEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("message.outboundProperties['foo']=='bar'");
    filter.setMuleContext(muleContext);
    MuleMessage message = MuleMessage.builder().payload("blah").build();
    assertTrue(!filter.accept(message, mock(MuleEvent.Builder.class)));

    message = MuleMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue(filter.accept(message, mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testVariableFilterEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("flowVars['foo']=='bar'");
    filter.setMuleContext(muleContext);
    MuleEvent event = getTestEvent("blah");
    assertTrue(!filter.accept(event, mock(MuleEvent.Builder.class)));
    event = MuleEvent.builder(event).addFlowVariable("foo", "bar").build();
    assertTrue(filter.accept(event, mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testHeaderFilterWithNotEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("message.outboundProperties['foo']!='bar'");
    filter.setMuleContext(muleContext);

    MuleMessage message = MuleMessage.builder().payload("blah").build();

    assertTrue(filter.accept(message, mock(MuleEvent.Builder.class)));
    message = MuleMessage.builder(message).addOutboundProperty("foo", "bar").build();
    assertTrue(!filter.accept(message, mock(MuleEvent.Builder.class)));
    message = MuleMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue(filter.accept(message, mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testVariableFilterWithNotEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("flowVars['foo']!='bar'");
    filter.setMuleContext(muleContext);

    MuleEvent event = getTestEvent("blah");

    assertTrue(filter.accept(event, mock(MuleEvent.Builder.class)));
    event = MuleEvent.builder(event).addFlowVariable("foo", "bar").build();
    assertTrue(!filter.accept(event, mock(MuleEvent.Builder.class)));
    event = MuleEvent.builder(event).addFlowVariable("foo", "car").build();
    assertTrue(filter.accept(event, mock(MuleEvent.Builder.class)));
  }

  private MuleMessage removeProperty(MuleMessage message) {
    return MuleMessage.builder(message).removeOutboundProperty("foo").build();
  }

  @Test
  public void testHeaderFilterWithNotNullEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("message.outboundProperties['foo']!=null");
    filter.setMuleContext(muleContext);

    MuleMessage message = MuleMessage.builder().payload("blah").build();

    assertTrue(!filter.accept(message, mock(MuleEvent.Builder.class)));
    message = removeProperty(message);
    assertTrue(!filter.accept(message, mock(MuleEvent.Builder.class)));
    message = MuleMessage.builder(message).addOutboundProperty("foo", "car").build();
    assertTrue(filter.accept(message, mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testVariableFilterWithNotNullEL() throws Exception {
    ExpressionFilter filter = new ExpressionFilter("flowVars['foo']!=null");
    filter.setMuleContext(muleContext);

    MuleEvent event = getTestEvent("blah");

    assertTrue(!filter.accept(event, mock(MuleEvent.Builder.class)));
    event = MuleEvent.builder(event).message(removeProperty(event.getMessage())).build();
    assertTrue(!filter.accept(event, mock(MuleEvent.Builder.class)));
    event = MuleEvent.builder(event).addFlowVariable("foo", "car").build();
    assertTrue(filter.accept(event, mock(MuleEvent.Builder.class)));
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

    ExpressionFilter filter = new ExpressionFilter("#[regex('The number is [1-9]')]");
    filter.setMuleContext(muleContext);

    assertNotNull(filter.getExpression());

    assertTrue(filter.accept(MuleMessage.builder().payload("The number is 4").build(), mock(MuleEvent.Builder.class)));
    assertFalse(filter.accept(MuleMessage.builder().payload("Say again?").build(), mock(MuleEvent.Builder.class)));

    assertFalse(filter.accept(MuleMessage.builder().payload("The number is 0").build(), mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testExceptionTypeFilter() {
    ExpressionFilter filter = new ExpressionFilter("exception is java.lang.Exception");
    filter.setMuleContext(muleContext);

    MuleMessage m = MuleMessage.builder().payload("test").build();
    assertTrue(!filter.accept(m, mock(MuleEvent.Builder.class)));
    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertTrue(filter.accept(m, mock(MuleEvent.Builder.class)));

    filter = new ExpressionFilter("exception is java.io.IOException");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(m, mock(MuleEvent.Builder.class)));
    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertTrue(filter.accept(m, mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testExceptionTypeFilterEL() {
    ExpressionFilter filter = new ExpressionFilter("exception is java.lang.Exception");
    filter.setMuleContext(muleContext);

    MuleMessage m = MuleMessage.builder().payload("test").build();
    assertTrue(!filter.accept(m, mock(MuleEvent.Builder.class)));
    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test"))).build();
    assertTrue(filter.accept(m, mock(MuleEvent.Builder.class)));

    filter = new ExpressionFilter("exception is java.io.IOException");
    filter.setMuleContext(muleContext);
    assertTrue(!filter.accept(m, mock(MuleEvent.Builder.class)));
    m = MuleMessage.builder(m).exceptionPayload(new DefaultExceptionPayload(new IOException("test"))).build();
    assertTrue(filter.accept(m, mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testPayloadTypeFilterEL() {
    ExpressionFilter filter = new ExpressionFilter("payload is org.mule.tck.testmodels.fruit.Apple");
    filter.setMuleContext(muleContext);

    assertTrue(filter.accept(MuleMessage.builder().payload(new Apple()).build(), mock(MuleEvent.Builder.class)));
    assertTrue(!filter.accept(MuleMessage.builder().payload("test").build(), mock(MuleEvent.Builder.class)));

    filter = new ExpressionFilter("payload is String");
    filter.setMuleContext(muleContext);
    assertTrue(filter.accept(MuleMessage.builder().payload("test").build(), mock(MuleEvent.Builder.class)));
    assertTrue(!filter.accept(MuleMessage.builder().payload(new Exception("test")).build(), mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testTrueStringEL() {
    ExpressionFilter filter = new ExpressionFilter("payload");
    filter.setMuleContext(muleContext);

    filter.setNullReturnsTrue(true);

    assertTrue(filter.accept(MuleMessage.builder().payload("true").build(), mock(MuleEvent.Builder.class)));
    assertTrue(filter.accept(MuleMessage.builder().payload("TRUE").build(), mock(MuleEvent.Builder.class)));
    assertTrue(filter.accept(MuleMessage.builder().payload("tRuE").build(), mock(MuleEvent.Builder.class)));
  }

  @Test
  public void testFalseStringEL() {
    ExpressionFilter filter = new ExpressionFilter("payload");
    filter.setMuleContext(muleContext);

    filter.setNullReturnsTrue(false);

    assertFalse(filter.accept(MuleMessage.builder().payload("false").build(), mock(MuleEvent.Builder.class)));
    assertFalse(filter.accept(MuleMessage.builder().payload("FALSE").build(), mock(MuleEvent.Builder.class)));
    assertFalse(filter.accept(MuleMessage.builder().payload("faLSe").build(), mock(MuleEvent.Builder.class)));
  }

}
