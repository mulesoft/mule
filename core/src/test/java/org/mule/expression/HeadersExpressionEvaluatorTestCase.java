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
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadersExpressionEvaluatorTestCase extends AbstractMuleTestCase
{
    private Map props;

    @Override
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

        result = eval.evaluate("fool*", message);
        assertNull(result);

        try
        {
            result = eval.evaluate("fool", message);
            fail("required value");
        }
        catch (Exception e)
        {
            //Expected
        }


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

        result = eval.evaluate("fool*", message);
        assertNull(result);

        try
        {
            result = eval.evaluate("fool", message);
            fail("required value");
        }
        catch (Exception e)
        {
            //Expected
        }

        //Test count
        assertEquals(3, eval.evaluate("#", message));

        //Test all
        result = eval.evaluate("*", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(3, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertTrue(((Map)result).values().contains("mar"));
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

        result = eval.evaluate("fool*", message);
        assertNull(result);

        try
        {
            result = eval.evaluate("fool", message);
            fail("required value");
        }
        catch (Exception e)
        {
            //Expected
        }
        
        //Test All
        result = eval.evaluate("*", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertTrue(((List)result).contains("mar"));
    }


    public void testSingleHeaderUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = muleContext.getExpressionManager().evaluate("#[header:foo]", message);
        assertNotNull(result);
        assertEquals("moo", result);

        result = muleContext.getExpressionManager().evaluate("#[header:fool*]", message);
        assertNull(result);

        try
        {
            result = muleContext.getExpressionManager().evaluate("#[header:fool]", message);
            fail("Required value");
        }
        catch (ExpressionRuntimeException e)
        {
            //exprected
        }

    }

    public void testMapHeadersUsingManager() throws Exception
    {

        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = muleContext.getExpressionManager().evaluate("#[headers:foo, baz]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertFalse(((Map)result).values().contains("mar"));

        result = muleContext.getExpressionManager().evaluate("#[headers:fool*]", message);
        assertNull(result);

        try
        {
            result = muleContext.getExpressionManager().evaluate("#[headers:fool]", message);
            fail("Required value");
        }
        catch (ExpressionRuntimeException e)
        {
            //exprected
        }

        assertEquals(3, muleContext.getExpressionManager().evaluate("#[headers:#]", message));
    }

    public void testListHeadersUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", props);

        Object result = muleContext.getExpressionManager().evaluate("#[headers-list:foo, baz]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertFalse(((List)result).contains("mar"));

        result = muleContext.getExpressionManager().evaluate("#[headers-list:fool*]", message);
        assertNull(result);

        try
        {
            result = muleContext.getExpressionManager().evaluate("#[headers-list:fool]", message);
            fail("Required value");
        }
        catch (ExpressionRuntimeException e)
        {
            //exprected
        }
    }
}
