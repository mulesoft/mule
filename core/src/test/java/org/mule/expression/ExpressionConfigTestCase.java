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

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.tck.AbstractMuleTestCase;

public class ExpressionConfigTestCase extends AbstractMuleTestCase
{
    public void testConfig() throws Exception
    {
        ExpressionConfig config = new ExpressionConfig("foo=bar", "header", null, "$[", "]");
        config.validate(muleContext.getExpressionManager());
        assertEquals("$[header:foo=bar]", config.getFullExpression(muleContext.getExpressionManager()));

        config = new ExpressionConfig("foo,bar", "headers", null);
        config.validate(muleContext.getExpressionManager());
        assertEquals("#[headers:foo,bar]", config.getFullExpression(muleContext.getExpressionManager()));

        config = new ExpressionConfig();
        config.parse("#[attachment:baz]");
        config.validate(muleContext.getExpressionManager());
        assertEquals("attachment", config.getEvaluator());
        assertEquals("baz", config.getExpression());
        assertNull(config.getCustomEvaluator());

    }

    public void testCustomConfig() throws Exception
    {
        muleContext.getExpressionManager().registerEvaluator(new ExpressionEvaluator()
        {
            public Object evaluate(String expression, MuleMessage message) { return null;  }

            public void setName(String name) { }

            public String getName() { return "customEval"; }
        });

        ExpressionConfig config = new ExpressionConfig("foo,bar", "custom", "customEval");
        config.validate(muleContext.getExpressionManager());
        assertEquals("#[customEval:foo,bar]", config.getFullExpression(muleContext.getExpressionManager()));
    }
}
