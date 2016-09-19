/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MVELMapHandlingTestCase extends AbstractMuleContextTestCase {

  private static final String KEY = "Name";
  private static final String VALUE = "MG";
  private ExpressionLanguage el;


  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    el = muleContext.getExpressionLanguage();
  }

  @Test
  public void keyWithNonNullValue() throws Exception {
    Map<String, String> payload = new HashMap<>();
    payload.put(KEY, VALUE);

    assertMapKey(payload, KEY, VALUE);
  }

  @Test
  public void keyWithNullValue() throws Exception {
    Map<String, String> payload = new HashMap<>();
    assertMapKey(payload, KEY, null);
  }

  @Test
  public void keyWithNullableValue() throws Exception {
    Map<String, String> payload = new HashMap<>();
    payload.put(KEY, VALUE);
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);

    Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(payload))
        .build();

    assertMapKey(event, KEY, VALUE);
    payload.remove(KEY);
    assertMapKey(event, KEY, null);
  }

  @Test
  public void nullKeyWhichGetsValueLater() throws Exception {
    Map<String, String> payload = new HashMap<>();
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);

    Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(payload))
        .build();

    assertMapKey(event, KEY, null);

    payload.put(KEY, VALUE);
    assertMapKey(event, KEY, VALUE);
  }

  private void assertMapKey(Object payload, String key, Object expectedValue) throws Exception {
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    assertMapKey(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(payload))
        .build(), key, expectedValue);
  }

  private void assertMapKey(Event event, String key, Object expectedValue) throws Exception {
    runExpressionAndExpect(String.format("#[payload.%s]", key), expectedValue, event);
    runExpressionAndExpect(String.format("#[payload['%s']]", key), expectedValue, event);
    runExpressionAndExpect(String.format("#[payload.'%s']", key), expectedValue, event);
  }

  private void runExpressionAndExpect(String expression, Object expectedValue, Event event) throws Exception {
    Object result = el.evaluate(expression, event, getTestFlow());
    assertThat(format("Expression %s returned unexpected value", expression), result, equalTo(expectedValue));
  }
}
