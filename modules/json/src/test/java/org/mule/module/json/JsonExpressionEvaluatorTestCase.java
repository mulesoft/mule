/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;

public class JsonExpressionEvaluatorTestCase extends AbstractMuleTestCase
{
    public void testExpressions() throws Exception
    {
        String json = IOUtils.getResourceAsString("test-data.json", getClass());
        MuleMessage message = new DefaultMuleMessage(json, muleContext);
        JsonExpressionEvaluator eval = new JsonExpressionEvaluator();

        assertEquals("test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492", eval.evaluate("[0]->text", message));

        assertEquals("Mule Test", eval.evaluate("[0]->user->name", message));
        assertEquals("Mule Test9", eval.evaluate("[9]->user->name", message));
        assertNull( eval.evaluate("[9]->user->XXX", message));
    }

    public void testExpressionsUsingManager() throws Exception
    {
        String json = IOUtils.getResourceAsString("test-data.json", getClass());
        MuleMessage message = new DefaultMuleMessage(json, muleContext);

        assertEquals("test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492", muleContext.getExpressionManager().evaluate("#[json:[0]->text]", message));

        assertEquals("Mule Test", muleContext.getExpressionManager().evaluate("json:[0]->user->name", message));
        assertEquals("Mule Test9", muleContext.getExpressionManager().evaluate("#[json:[9]->user->name]", message));
        assertNull( muleContext.getExpressionManager().evaluate("json:[9]->user->XXX", message, false));

        try
        {
            muleContext.getExpressionManager().evaluate("json:[9]->user->XXX", message, true);
            fail("A value was required");
        }
        catch (ExpressionRuntimeException e)
        {
            //Expected
        }
    }

    public void testExpressionFilter() throws Exception
    {
        String json = IOUtils.getResourceAsString("test-data.json", getClass());
        MuleMessage message = new DefaultMuleMessage(json, muleContext);

        ExpressionFilter filter = new ExpressionFilter("#[json:[0]->text]");
        filter.setMuleContext(muleContext);
        assertTrue(filter.accept(message));

        filter.setExpression("[0]->favorited");
        assertFalse(filter.accept(message));

        filter.setExpression("[0]->truncated");
        assertTrue(filter.accept(message));

        filter.setExpression("[0]->source");
        assertTrue(filter.accept(message));

        filter.setExpression("[0]->xxx");
        assertFalse(filter.accept(message));
    }

    public void testExpressionFilterWithBooleanLogic() throws Exception
    {
        String json = IOUtils.getResourceAsString("test-data.json", getClass());
        MuleMessage message = new DefaultMuleMessage(json, muleContext);

        ExpressionFilter filter = new ExpressionFilter("#[json:[0]->text]");
        filter.setMuleContext(muleContext);
        assertTrue(filter.accept(message));

        filter.setExpression("[0]->favorited==false");
        assertTrue(filter.accept(message));

        filter.setExpression("[0]->truncated != true");
        assertFalse(filter.accept(message));

        filter.setExpression("[0]->source==null");
        assertFalse(filter.accept(message));

        filter.setExpression("[0]->source!= null");
        assertTrue(filter.accept(message));
    }
}
