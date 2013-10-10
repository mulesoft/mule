/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class VariableEnricherEvaluatorTestCase extends AbstractMuleContextTestCase
{

    @Test
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

    @Test
    public void testEnrichEvaluateWithManager() throws Exception
    {
        ExpressionManager expressionManager = muleContext.getExpressionManager();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        expressionManager.enrich("#[variable:foo]", message, "fooValue");

        // Value required + found
        Object result = expressionManager.evaluate("#[variable:foo]", message);
        assertNotNull(result);
        assertEquals("fooValue", result);

        // Value required + not found (throws exception)
        try
        {
            expressionManager.evaluate("#[variable:fool]", message);
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

}
