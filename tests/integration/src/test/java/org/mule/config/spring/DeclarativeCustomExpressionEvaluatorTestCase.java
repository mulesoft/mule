/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.expression.CustomExpressionEvaluatorTestCase;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DeclarativeCustomExpressionEvaluatorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
