/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertTrue;

import org.mule.expression.CustomExpressionEvaluatorTestCase;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class DeclarativeCustomExpressionEvaluatorTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/declarative-custom-expression-evaluator-test.xml";
    }

    @Test
    public void testCustomEvalutorRegistered()
    {
        CustomExpressionEvaluatorTestCase.FooExpressionEvaluator customEvaluator =
                new CustomExpressionEvaluatorTestCase.FooExpressionEvaluator();
        assertTrue("Custom evaluator has not been registered",
                   muleContext.getExpressionManager().isEvaluatorRegistered(customEvaluator.getName()));
    }
}
