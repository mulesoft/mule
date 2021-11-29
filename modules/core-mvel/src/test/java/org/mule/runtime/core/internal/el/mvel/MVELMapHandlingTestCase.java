/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class MVELMapHandlingTestCase extends AbstractMuleContextTestCase {

  private static final String KEY = "Name";
  private static final String VALUE = "MG";

  private ExpressionManager expressionManager;

  @Before
  public void configureExpressionManager() throws MuleException {
    expressionManager = muleContext.getExpressionManager();
    ((DefaultExpressionManager) expressionManager).setMelDefault(true);
    ((DefaultExpressionManager) expressionManager)
        .setExpressionLanguage((((MuleContextWithRegistry) muleContext).getRegistry()).lookupObject(OBJECT_EXPRESSION_LANGUAGE));
  }

  @Override
  protected boolean mockExprExecutorService() {
    return true;
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

  @Test
  public void map() throws Exception {
    Map<String, String> payload = new HashMap<>();
    CoreEvent event = eventBuilder(muleContext).message(of(payload)).build();
    Map result = (Map) expressionManager.evaluate("#[mel:{\"a\" : {\"b\" : \"c\"}, \"d\" : [\"e\"]}]", event).getValue();
    Map result2 = (Map) expressionManager.evaluate("#[mel:{\"d\" : [\"e\"], \"a\" : {\"b\" : \"c\"}}]", event).getValue();
    assertThat(((ArrayList<String>) result.get("d")).get(0), equalTo("e"));
    assertThat(((ArrayList<String>) result2.get("d")).get(0), equalTo("e"));
  }

  private void runExpressionAndExpect(String expression, Object expectedValue, CoreEvent event) throws Exception {
    Object result = expressionManager.evaluate(expression, event, TEST_CONNECTOR_LOCATION).getValue();
    assertThat(format("Expression %s returned unexpected value", expression), result, equalTo(expectedValue));
  }
}
