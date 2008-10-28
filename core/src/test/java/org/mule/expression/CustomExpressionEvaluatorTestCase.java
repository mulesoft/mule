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
import org.mule.api.transport.MessageAdapter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.expression.ExpressionEvaluator;
import org.mule.util.expression.ExpressionEvaluatorManager;

import java.util.Map;

/**
 * Tests a custom expression evaluator using direct registration with the manager.
 */
public class CustomExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    public void testCustomExpressionEvaluator()
    {
        ExpressionEvaluatorManager.registerEvaluator(new FooExpressionEvaluator());

        Object result = ExpressionEvaluatorManager.evaluate("#[foo:abc]", 
            new DefaultMuleMessage("test", (Map) null));
        assertNotNull(result);
        assertEquals("Wrong evaluation result", "testabc", result);
    }

    public static class FooExpressionEvaluator implements ExpressionEvaluator
    {

        public Object evaluate(String expression, MessageAdapter message)
        {
            return message.getPayload() + expression;
        }

        public void setName(String name)
        {
            throw new UnsupportedOperationException("setName");
        }

        public String getName()
        {
            return "foo";
        }
    }

}
