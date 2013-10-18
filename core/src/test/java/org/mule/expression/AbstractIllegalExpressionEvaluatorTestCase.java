/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
