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
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessageHeadersListExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private HashMap<String, Object> messageProperties;
    private MessageHeadersListExpressionEvaluator evaluator = new MessageHeadersListExpressionEvaluator();
    private MuleMessage message;

    public MessageHeadersListExpressionEvaluatorTestCase()
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
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertFalse(list.contains("barvalue"));
    }

    @Test
    public void requiredHeadersWithExistingValuesViaExpressionManagerShouldReturnValue()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:foo, baz]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertFalse(list.contains("barvalue"));
    }

    @Test(expected = RequiredValueException.class)
    public void requiredHeadersWithMissingValuesShouldFail()
    {
        evaluator.evaluate("nonexistent", message);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredHeadersWithMissingValuesViaExpressionManagerShouldFail()
    {
        muleContext.getExpressionManager().evaluate("#[headers-list:nonexistent]", message);
    }

    @Test
    public void optionalHeadersWithExistingValuesShouldReturnValues()
    {
        Object result = evaluator.evaluate("foo?, baz?", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertFalse(list.contains("barvalue"));
    }

    @Test
    public void optionalHeadersWithExistingValuesViaExpressionManagerShouldReturnValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:foo?, baz]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertFalse(list.contains("barvalue"));

    }

    @Test
    public void optionalHeadersWithMissingValuesShouldReturnEmptyList() throws Exception
    {
        Object result = evaluator.evaluate("fool?", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void optionalHeadersWithMissingValuesViaExpressionManagerShouldReturnEmptyList()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:nonexistent?]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void requiredHeadersWithExplicitScopeShouldReturnValues()
    {
        Object result = evaluator.evaluate("OUTBOUND:foo, OUTBOUND:baz", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertFalse(list.contains("barvalue"));
    }

    @Test
    public void propertyScopeSpecifiedForOneKeyShouldSetScopeForAllOtherKeys()
    {
        // this is equivalent to OUTBOUND:foo, OUTBOUND,:baz
        Object result = evaluator.evaluate("OUTBOUND:foo, baz", message);
        assertNotNull(result);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
    }

    @Test
    public void propertiesFromDifferentScopesWithValuesShouldReturnValues()
    {
        message.setProperty("faz", "fazvalue", PropertyScope.INVOCATION);

        Object result = evaluator.evaluate("OUTBOUND:foo, OUTBOUND:baz, INVOCATION:faz", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertTrue(list.contains("fazvalue"));
    }

    @Test
    public void matchAllWildcardShouldReturnAllHeaderValues()
    {
        Object result = evaluator.evaluate("*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertTrue(list.contains("barvalue"));
    }

    @Test
    public void matchAllWildcardViaExpressionManagerShouldReturnAllHeaderValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertTrue(list.contains("barvalue"));
    }

    @Test
    public void matchBeginningWildcardShouldReturnValues()
    {
        Object result = evaluator.evaluate("ba*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("barvalue"));
        assertTrue(list.contains("bazvalue"));
    }

    @Test
    public void matchBeginningWildcardViaExpressionManagerShouldReturnValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:ba*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertFalse(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertTrue(list.contains("barvalue"));
    }

    @Test
    public void wildcardWithNoMatchShouldReturnEmptyList()
    {
        Object result = evaluator.evaluate("x*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void wildcardWithNoMatchViaExpressionManagerShouldReturnEmptyList()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:x*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void multipleWildcardsShouldReturnValues() throws Exception
    {
        Object result = evaluator.evaluate("ba*, f*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertTrue(list.contains("barvalue"));
    }

    @Test
    public void multipleWildcardsViaExpressionManagerShouldReturnValues()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:ba*, f*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertTrue(list.contains("foovalue"));
        assertTrue(list.contains("bazvalue"));
        assertTrue(list.contains("barvalue"));
    }
}
