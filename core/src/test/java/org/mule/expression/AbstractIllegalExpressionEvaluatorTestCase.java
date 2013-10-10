/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

/**
 * Contract test for {@link IllegalExpressionEvaluator}
 */
public abstract class AbstractIllegalExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    @Test
    public final void cannotBeEvaluated()
    {
        IllegalExpressionEvaluator evaluator = createIllegalExpressionEvaluator();

        try
        {
            evaluator.evaluate("", null);
            fail("Evaluator should fail");
        }
        catch (UnsupportedOperationException e)
        {
            assertEquals(e.getMessage(), String.format(IllegalExpressionEvaluator.ILLEGAL_EVALUATOR_MESSAGE, evaluator.getName()));
        }
    }

    protected abstract IllegalExpressionEvaluator createIllegalExpressionEvaluator();
}
