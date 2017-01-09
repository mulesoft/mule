/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class VariablesTestCase extends AbstractELTestCase {

  private Event event;

  public VariablesTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Before
  public void setup() throws Exception {
    event = Event.builder(context)
        .message(InternalMessage.of(""))
        .build();
  }

  @Test
  public void flowVariablesMap() throws Exception {
    event = Event.builder(event).addVariable("foo", "bar").build();
    assertTrue(evaluate("flowVars", event) instanceof Map);
  }

  @Test
  public void assignToFlowVariablesMap() throws Exception {
    assertImmutableVariable("flowVars='foo'", event);
  }

  @Test
  public void flowVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct)
        .addVariable("foo", "bar").build();
    assertEquals(event.getVariable("foo").getValue(), evaluate("flowVars['foo']", event));
  }

  @Test
  public void assignValueToFlowVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct)
        .addVariable("foo", "bar_old").build();
    Event.Builder eventBuilder = Event.builder(event);
    evaluate("flowVars['foo']='bar'", event, eventBuilder);
    assertEquals("bar", eventBuilder.build().getVariable("foo").getValue());
  }

  @Test
  public void assignValueToNewFlowVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    Event.Builder eventBuilder = Event.builder(event);
    evaluate("flowVars['foo']='bar'", event, eventBuilder);
    assertEquals("bar", eventBuilder.build().getVariable("foo").getValue());
  }

  @Test
  public void sessionVariablesMap() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertTrue(evaluate("sessionVars", event) instanceof Map);
  }

  @Test
  public void assignToSessionVariablesMap() throws Exception {
    assertImmutableVariable("sessionVars='foo'", event);
  }

  @Test
  public void sessionVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertEquals(event.getSession().getProperty("foo"), evaluate("sessionVars['foo']", event));
  }

  @Test
  public void assignValueToSessionVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar_old");
    evaluate("sessionVars['foo']='bar'", event);
    assertEquals("bar", event.getSession().getProperty("foo"));
  }

  @Test
  public void assignValueToNewSessionVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    evaluate("sessionVars['foo']='bar'", event);
    assertEquals("bar", event.getSession().getProperty("foo"));
  }

  @Test
  public void variableFromFlowScope() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct)
        .addVariable("foo", "bar").build();
    event.getSession().setProperty("foo", "NOTbar");
    assertEquals(event.getVariable("foo").getValue(), evaluate("foo", event));
  }

  @Test
  public void updateVariableFromFlowScope() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct)
        .addVariable("foo", "bar").build();
    assertEquals("bar_new", evaluate("foo='bar_new'", event));
  }

  @Test
  public void variableFromSessionScope() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertEquals(event.getSession().getProperty("foo"), evaluate("foo", event));
  }

  @Test
  public void updateVariableFromSessionScope() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertEquals("bar_new", evaluate("foo='bar_new'", event));
  }

  @Test
  public void assignValueToVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct)
        .addVariable("foo", "bar_old").build();
    Event.Builder eventBuilder = Event.builder(event);
    evaluate("foo='bar'", event, eventBuilder);
    assertEquals("bar", eventBuilder.build().getVariable("foo").getValue());
  }

  @Test
  public void assignValueToLocalVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    evaluate("localVar='bar'", event);
  }

  /**
   * See MULE-6381
   */
  @Test
  public void reassignValueToLocalVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    evaluate("localVar='bar';localVar='bar2'", event);
  }

  @Test
  public void localVariable() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    assertEquals("bar", evaluate("localVar='bar';localVar", event));
  }
}
