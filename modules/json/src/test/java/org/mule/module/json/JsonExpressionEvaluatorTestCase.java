/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsonExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    protected JsonExpressionEvaluator eval;
    protected String evalName;
    protected MuleMessage message;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        eval = getEvaluator();
        evalName = getEvaluatorName();
        message = new DefaultMuleMessage(IOUtils.getResourceAsString("test-data.json", getClass()),
            muleContext);
    }

    protected JsonExpressionEvaluator getEvaluator()
    {
        return new JsonExpressionEvaluator();
    }

    protected String getEvaluatorName()
    {
        return "json";
    }

    @Test
    public void testExpressions() throws Exception
    {
        assertEquals("test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492", eval.evaluate("[0]/text",
            message));

        assertEquals("Mule Test", eval.evaluate("[0]/user/name", message));
        assertEquals("Mule Test9", eval.evaluate("[9]/'user'/name", message));
        assertNull(eval.evaluate("[9]/user/XXX", message));
    }

    @Test
    public void testReturnTypes()
    {
        // String
        assertEquals(String.class, eval.evaluate("[0]/user/name", message).getClass());
        // Number
        assertEquals(String.class, eval.evaluate("[0]/id", message).getClass());
        // Boolean
        assertEquals(String.class, eval.evaluate("[0]/truncated", message).getClass());
        // Object
        assertEquals(String.class, eval.evaluate("[0]/user", message).getClass());
        // Array
        assertEquals(ArrayList.class, eval.evaluate("[0]/anArray", message).getClass());
        assertEquals(String.class, ((List) eval.evaluate("[0]/anArray", message)).get(0).getClass());
        assertEquals(String.class, ((List) eval.evaluate("[0]/anArray", message)).get(1).getClass());
        assertEquals(ArrayList.class, ((List) eval.evaluate("[0]/anArray", message)).get(2).getClass());
    }

    @Test
    public void testExpressionsUsingManager() throws Exception
    {
        assertEquals("test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492",
            muleContext.getExpressionManager().evaluate("#[json:[0]/text]", message));

        assertEquals("Mule Test", muleContext.getExpressionManager().evaluate(evalName + ":[0]/user/name",
            message));
        assertEquals("Mule Test9", muleContext.getExpressionManager().evaluate("#[json:[9]/'user'/name]",
            message));
        assertNull(muleContext.getExpressionManager().evaluate(evalName + ":[9]/user/XXX", message, false));

        try
        {
            muleContext.getExpressionManager().evaluate(evalName + ":[9]/user/XXX", message, true);
            fail("A value was required");
        }
        catch (ExpressionRuntimeException e)
        {
            // Expected
        }
    }

    @Test
    public void testExpressionFilter() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("#[json:[0]/text]");
        filter.setMuleContext(muleContext);
        assertTrue(filter.accept(message));

        filter.setExpression("[0]/favorited");
        assertFalse(filter.accept(message));

        filter.setExpression("[0]/truncated");
        assertTrue(filter.accept(message));

        filter.setExpression("[0]/source");
        assertTrue(filter.accept(message));

        filter.setExpression("[0]/xxx");
        assertFalse(filter.accept(message));
    }

    @Test
    public void testExpressionFilterWithBooleanLogic() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("#[json:[0]/text]");
        filter.setMuleContext(muleContext);
        assertTrue(filter.accept(message));

        filter.setExpression("[0]/favorited=false");
        assertTrue(filter.accept(message));

        filter.setExpression("[0]/truncated != true");
        assertFalse(filter.accept(message));

        filter.setExpression("[0]/source=null");
        assertFalse(filter.accept(message));

        filter.setExpression("[0]/source!= null");
        assertTrue(filter.accept(message));

    }

    @Test
    public void testExpressionFilterWithBooleanLogicWhereElementDoesNotExist() throws Exception
    {
        // Checks against elements that do not exist

        ExpressionFilter filter = new ExpressionFilter("#[json:[0]/xyz = null]");
        filter.setMuleContext(muleContext);
        assertTrue(filter.accept(message));

        filter.setExpression("[0]/xyz!= null");
        assertFalse(filter.accept(message));
    }
}
