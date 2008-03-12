/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.expression.MessageHeaderExpressionEvaluator;
import org.mule.util.expression.MessageHeadersExpressionEvaluator;
import org.mule.util.expression.MessageHeadersListExpressionEvaluator;
import org.mule.util.expression.ExpressionEvaluatorManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadersExpressionEvaluatorTestCase extends AbstractMuleTestCase
{
    private Map props;

    //@Override
    public void doSetUp()
    {
        props = new HashMap(3);
        props.put("foo", "moo");
        props.put("bar", "mar");
        props.put("baz", "maz");
    }

    public void testSingleHeader() throws Exception
    {
        MessageHeaderExpressionEvaluator eval = new MessageHeaderExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = eval.evaluate("foo", message);
        assertNotNull(result);
        assertEquals("moo", result);

        result = eval.evaluate("fool", message);
        assertNull(result);

        result = eval.evaluate("foo", new Object());
        assertNull(result);

    }

    public void testMapHeaders() throws Exception
    {
        MessageHeadersExpressionEvaluator eval = new MessageHeadersExpressionEvaluator();

        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = eval.evaluate("foo, baz", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertFalse(((Map)result).values().contains("mar"));

        result = eval.evaluate("fool", message);
        assertNull(result);

        result = eval.evaluate("foo", new Object());
        assertNull(result);

    }

    public void testListHeaders() throws Exception
    {
        MessageHeadersListExpressionEvaluator eval = new MessageHeadersListExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = eval.evaluate("foo, baz", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertFalse(((List)result).contains("mar"));

        result = eval.evaluate("fool", message);
        assertNull(result);

        result = eval.evaluate("foo", new Object());
        assertNull(result);

    }


    public void testSingleHeaderUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = ExpressionEvaluatorManager.evaluate("${header:foo}", message);
        assertNotNull(result);
        assertEquals("moo", result);

        result = ExpressionEvaluatorManager.evaluate("${header:fool}", message);
        assertNull(result);

        result = ExpressionEvaluatorManager.evaluate("${header:foo}", new Object());
        assertNull(result);

    }

    public void testMapHeadersUsingManager() throws Exception
    {

        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = ExpressionEvaluatorManager.evaluate("${headers:foo, baz}", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertFalse(((Map)result).values().contains("mar"));

        result = ExpressionEvaluatorManager.evaluate("${headers:fool}", message);
        assertNull(result);

        result = ExpressionEvaluatorManager.evaluate("${headers:foo}", new Object());
        assertNull(result);

    }

    public void testListHeadersUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = ExpressionEvaluatorManager.evaluate("${headers-list:foo, baz}", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertFalse(((List)result).contains("mar"));

        result = ExpressionEvaluatorManager.evaluate("${headers-list:fool}", message);
        assertNull(result);

        result = ExpressionEvaluatorManager.evaluate("${headers-list:foo}", new Object());
        assertNull(result);

    }
}
