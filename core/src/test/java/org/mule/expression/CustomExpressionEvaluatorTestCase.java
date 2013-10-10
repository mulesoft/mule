/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

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

        public String getName()
        {
            return "foo";
        }
    }

}
