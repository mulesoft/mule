/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageHeadersExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private HashMap<String, Object> messageProperties;
    private MessageHeadersExpressionEvaluator evaluator = new MessageHeadersExpressionEvaluator();
    private MuleMessage message;

    public MessageHeadersExpressionEvaluatorTestCase()
    {
        super();
        setDisposeContextPerClass(true);
    }

    @Override
    public void doSetUp()
    {
        messageProperties = new HashMap<String, Object>(3);
        messageProperties.put("foo", "foovalue");
        messageProperties.put("bar", "barvalue");
        messageProperties.put("baz", "bazvalue");

        message = new DefaultMuleMessage(TEST_MESSAGE, messageProperties, muleContext);
    }

    @Test
    public void requiredHeadersWithExitingValueShouldReturnValue()
    {
        Object result = evaluator.evaluate("foo, baz", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(2, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertFalse(map.values().contains("barvalue"));
    }

    @Test
    public void requiredHeadersWithExistingValuesViaExpressionManagerShouldReturnValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers:foo, baz]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(2, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertFalse(map.values().contains("barvalue"));
    }

    @Test(expected = RequiredValueException.class)
    public void requiredHeadersWithMissingValuesShouldFail()
    {
        evaluator.evaluate("OUTBOUND:foo, baz, faz", message);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredHeadersWithMissingValuesViaExpressionManagerShouldFail()
    {
        muleContext.getExpressionManager().evaluate("#[headers:nonexistent]", message);
    }

    @Test
    public void optionalHeadersWithExistingValuesShouldReturnValues()
    {
        Object result = evaluator.evaluate("foo?, baz", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(2, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertFalse(map.values().contains("barvalue"));
    }

    @Test
    public void optionalHeadersWithExistingValuesViaExpressionManagerShouldReturnValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers:foo?, baz]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(2, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertFalse(map.values().contains("barvalue"));
    }

    @Test
    public void optionalHeadersWithMissingValuesShouldReturnEmptyMap()
    {
        Object result = evaluator.evaluate("fool?", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(0, map.size());
    }

    @Test
    public void optionalHeadersWithMissingValuesViaExpressionManagerShouldReturnEmptyMap()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers:nonexistent?]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(0, map.size());
    }

    @Test
    public void requiredHeadersWithExplicitScopeShouldReturnValues()
    {
        Object result = evaluator.evaluate("OUTBOUND:foo, OUTBOUND:baz", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
    }

    @Test
    public void propertyScopeSpecifiedForOneKeyShouldSetScopeForAllOtherKeys()
    {
        // this is equivalent to OUTBOUND:foo, OUTBOUND:baz
        Object result = evaluator.evaluate("OUTBOUND:foo, baz", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
    }

    @Test
    public void propertiesFromDifferentScopesWithValuesShouldReturnValues()
    {
        message.setProperty("faz", "fazvalue", PropertyScope.INVOCATION);

        Object result = evaluator.evaluate("OUTBOUND:foo, OUTBOUND:baz, INVOCATION:faz", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertTrue(map.values().contains("fazvalue"));
    }

    @Test
    public void matchAllWildcardShouldReturnAllHeaderValues() throws Exception
    {
        Object result = evaluator.evaluate("*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(3, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertTrue(map.values().contains("barvalue"));
    }

    @Test
    public void matchAllWildcardViaExpressionManagerShouldReturnAllHeaderValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers:*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(3, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertTrue(map.values().contains("barvalue"));
    }

    @Test
    public void matchBeginningWildcardShouldReturnValues()
    {
        Object result = evaluator.evaluate("ba*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(2, map.size());
        assertFalse(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertTrue(map.values().contains("barvalue"));
    }

    @Test
    public void matchBeginningWildcardViaExpressionManagerShouldReturnValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers:ba*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(2, map.size());
        assertFalse(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertTrue(map.values().contains("barvalue"));
    }

    @Test
    public void wildcardWithNoMatchShouldReturnEmptyMap()
    {
        Object result = evaluator.evaluate("x*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(0, map.size());
    }

    @Test
    public void wildcardWithnoMatchViaExpressionManagerShouldReturnEmptyMap()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers:x*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(0, map.size());
    }

    @Test
    public void multipleWildcardsShouldReturnValues() throws Exception
    {
        Object result = evaluator.evaluate("ba*, f*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(3, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertTrue(map.values().contains("barvalue"));
    }

    @Test
    public void multipleWildcardsViaExpressionManagerShouldReturnValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers:ba*, f*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>)result;
        assertEquals(3, map.size());
        assertTrue(map.values().contains("foovalue"));
        assertTrue(map.values().contains("bazvalue"));
        assertTrue(map.values().contains("barvalue"));
    }
}
