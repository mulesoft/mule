/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;

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
    event = Event.builder(context).message(of("")).build();
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
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct)
        .addVariable("foo", "bar").build();
    assertEquals(event.getVariables().get("foo").getValue(), evaluate("flowVars['foo']", event));
  }

  @Test
  public void assignValueToFlowVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct)
        .addVariable("foo", "bar_old").build();
    Event.Builder eventBuilder = Event.builder(event);
    evaluate("flowVars['foo']='bar'", event, eventBuilder);
    assertEquals("bar", eventBuilder.build().getVariables().get("foo").getValue());
  }

  @Test
  public void assignValueToNewFlowVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    Event.Builder eventBuilder = Event.builder(event);
    evaluate("flowVars['foo']='bar'", event, eventBuilder);
    assertEquals("bar", eventBuilder.build().getVariables().get("foo").getValue());
  }

  @Test
  public void sessionVariablesMap() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertTrue(evaluate("sessionVars", event) instanceof Map);
  }

  @Test
  public void assignToSessionVariablesMap() throws Exception {
    assertImmutableVariable("sessionVars='foo'", event);
  }

  @Test
  public void sessionVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertEquals(event.getSession().getProperty("foo"), evaluate("sessionVars['foo']", event));
  }

  @Test
  public void assignValueToSessionVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar_old");
    evaluate("sessionVars['foo']='bar'", event);
    assertEquals("bar", event.getSession().getProperty("foo"));
  }

  @Test
  public void assignValueToNewSessionVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    evaluate("sessionVars['foo']='bar'", event);
    assertEquals("bar", event.getSession().getProperty("foo"));
  }

  @Test
  public void variableFromFlowScope() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct)
        .addVariable("foo", "bar").build();
    event.getSession().setProperty("foo", "NOTbar");
    assertEquals(event.getVariables().get("foo").getValue(), evaluate("foo", event));
  }

  @Test
  public void updateVariableFromFlowScope() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct)
        .addVariable("foo", "bar").build();
    assertEquals("bar_new", evaluate("foo='bar_new'", event));
  }

  @Test
  public void variableFromSessionScope() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertEquals(event.getSession().getProperty("foo"), evaluate("foo", event));
  }

  @Test
  public void updateVariableFromSessionScope() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    event.getSession().setProperty("foo", "bar");
    assertEquals("bar_new", evaluate("foo='bar_new'", event));
  }

  @Test
  public void assignValueToVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct)
        .addVariable("foo", "bar_old").build();
    Event.Builder eventBuilder = Event.builder(event);
    evaluate("foo='bar'", event, eventBuilder);
    assertEquals("bar", eventBuilder.build().getVariables().get("foo").getValue());
  }

  @Test
  public void assignValueToLocalVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    evaluate("localVar='bar'", event);
  }

  /**
   * See MULE-6381
   */
  @Test
  public void reassignValueToLocalVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    evaluate("localVar='bar';localVar='bar2'", event);
  }

  @Test
  public void localVariable() throws Exception {
    Message message = of("");
    Event event = Event.builder(context).message(message).flow(flowConstruct).build();
    assertEquals("bar", evaluate("localVar='bar';localVar", event));
  }
}
