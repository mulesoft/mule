/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MVELMapHandlingTestCase extends AbstractMuleContextTestCase {

  private static final String KEY = "Name";
  private static final String VALUE = "MG";
  private ExpressionManager el;


  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    el = muleContext.getExpressionManager();
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

    CoreEvent event = eventBuilder(muleContext).message(of(payload)).build();

    assertMapKey(event, KEY, VALUE);
    payload.remove(KEY);
    assertMapKey(event, KEY, null);
  }

  @Test
  public void nullKeyWhichGetsValueLater() throws Exception {
    Map<String, String> payload = new HashMap<>();

    CoreEvent event = eventBuilder(muleContext).message(of(payload)).build();

    assertMapKey(event, KEY, null);

    payload.put(KEY, VALUE);
    assertMapKey(event, KEY, VALUE);
  }

  private void assertMapKey(Object payload, String key, Object expectedValue) throws Exception {
    assertMapKey(eventBuilder(muleContext).message(of(payload)).build(), key, expectedValue);
  }

  private void assertMapKey(CoreEvent event, String key, Object expectedValue) throws Exception {
    runExpressionAndExpect(String.format("#[mel:payload.%s]", key), expectedValue, event);
    runExpressionAndExpect(String.format("#[mel:payload['%s']]", key), expectedValue, event);
    runExpressionAndExpect(String.format("#[mel:payload.'%s']", key), expectedValue, event);
  }

  private void runExpressionAndExpect(String expression, Object expectedValue, CoreEvent event) throws Exception {
    Object result = el.evaluate(expression, event, TEST_CONNECTOR_LOCATION).getValue();
    assertThat(format("Expression %s returned unexpected value", expression), result, equalTo(expectedValue));
  }
}
