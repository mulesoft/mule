/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;

public class VariableEnricherEvaluatorTestCase extends AbstractMuleTestCase
{
    public void testEnrichEvaluate() throws Exception
    {
        VariableExpressionEvaluator eval = new VariableExpressionEvaluator();
        VariableExpressionEnricher enricher = new VariableExpressionEnricher();

        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        enricher.enrich("foo", message, "fooValue");

        // Value required + found
        Object result = eval.evaluate("foo", message);
        assertNotNull(result);
        assertEquals("fooValue", result);

        // Value required + not found (throws exception)
        try
        {
            eval.evaluate("fool", message);
            fail("required value");
        }
        catch (Exception e)
        {
            // Expected
        }

        // Variable is stored as an invocation property
        result = message.getProperty("foo", PropertyScope.INVOCATION);
        assertNotNull(result);
        assertEquals("fooValue", result);
    }

    public void testEnrichEvaluateWithManager() throws Exception
    {
        ExpressionManager expressionManager = muleContext.getExpressionManager();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        expressionManager.enrich("#[variable:foo]", message, "fooValue");

        // Value required + found
        Object result = expressionManager.evaluate("#[variable:foo]", message);
        assertNotNull(result);
        assertEquals("fooValue", result);

        // Value required + not found (returns null)
        assertNull(expressionManager.evaluate("#[variable:foo]", message));

        // Variable is stored as an invocation property
        result = message.getProperty("foo", PropertyScope.INVOCATION);
        assertNotNull(result);
        assertEquals("fooValue", result);

    }

}
