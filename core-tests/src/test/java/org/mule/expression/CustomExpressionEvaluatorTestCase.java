/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.TypedValue;

import java.util.Map;

import org.junit.Test;

/**
 * Tests a custom expression evaluator using direct registration with the manager.
 */
public class CustomExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testCustomExpressionEvaluator()
    {
        muleContext.getExpressionManager().registerEvaluator(new FooExpressionEvaluator());

        Object result = muleContext.getExpressionManager().evaluate("#[foo:abc]",
            new DefaultMuleMessage("test", (Map) null, muleContext));
        assertNotNull(result);
        assertEquals("Wrong evaluation result", "testabc", result);
    }

    public static class FooExpressionEvaluator implements ExpressionEvaluator
    {

        public Object evaluate(String expression, MuleMessage message)
        {
            return message.getPayload() + expression;
        }

        @Override
        public TypedValue evaluateTyped(String expression, MuleMessage message)
        {
            return null;
        }

        public String getName()
        {
            return "foo";
        }
    }

}
