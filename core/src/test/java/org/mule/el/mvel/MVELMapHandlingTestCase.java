/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.el.ExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MVELMapHandlingTestCase extends AbstractMuleContextTestCase
{

    private static final String KEY = "Name";
    private static final String VALUE = "MG";
    private ExpressionLanguage el;


    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        el = muleContext.getExpressionLanguage();
    }

    @Test
    public void keyWithNonNullValue() throws Exception
    {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put(KEY, VALUE);

        assertMapKey(payload, KEY, VALUE);
    }

    @Test
    public void keyWithNullValue() throws Exception
    {
        Map<String, String> payload = new HashMap<String, String>();
        assertMapKey(payload, KEY, null);
    }

    @Test
    public void keyWithNullableValue() throws Exception
    {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put(KEY, VALUE);

        MuleEvent event = getTestEvent(payload);

        assertMapKey(event, KEY, VALUE);
        payload.remove(KEY);
        assertMapKey(event, KEY, null);
    }

    @Test
    public void nullKeyWhichGetsValueLater() throws Exception
    {
        Map<String, String> payload = new HashMap<String, String>();

        MuleEvent event = getTestEvent(payload);

        assertMapKey(event, KEY, null);

        payload.put(KEY, VALUE);
        assertMapKey(event, KEY, VALUE);
    }

    private void assertMapKey(Object payload, String key, Object expectedValue) throws Exception
    {
        assertMapKey(getTestEvent(payload), key, expectedValue);
    }

    private void assertMapKey(MuleEvent event, String key, Object expectedValue) throws Exception
    {
        runExpressionAndExpect(String.format("#[payload.%s]", key), expectedValue, event);
        runExpressionAndExpect(String.format("#[payload['%s']]", key), expectedValue, event);
        runExpressionAndExpect(String.format("#[payload.'%s']", key), expectedValue, event);
    }
    
    @Test
    public void map() throws Exception
    {
        Map<String, String> payload = new HashMap<String, String>();
        MuleEvent event = getTestEvent(payload);
        Map result = (Map) el.evaluate("#[{\"a\" : {\"b\" : \"c\"}, \"d\" : [\"e\"]}]", event);
        Map result2 = (Map) el.evaluate("#[{\"d\" : [\"e\"], \"a\" : {\"b\" : \"c\"}}]", event);
        assertThat((String) ((ArrayList) result.get("d")).get(0), equalTo("e"));
        assertThat((String) ((ArrayList) result2.get("d")).get(0), equalTo("e"));
    }

    private void runExpressionAndExpect(String expression, Object expectedValue, MuleEvent event)
    {
        Object result = el.evaluate(expression, event);
        assertThat(String.format("Expression %s returned unexpected value", expression), result, equalTo(expectedValue));
    }
}
