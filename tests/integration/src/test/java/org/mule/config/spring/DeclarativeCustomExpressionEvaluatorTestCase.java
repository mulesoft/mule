package org.mule.config.spring;

import org.mule.expression.CustomExpressionEvaluatorTestCase;
import org.mule.tck.FunctionalTestCase;

public class DeclarativeCustomExpressionEvaluatorTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/declarative-custom-expression-evaluator-test.xml";
    }

    public void testCustomEvalutorRegistered()
    {
        CustomExpressionEvaluatorTestCase.FooExpressionEvaluator customEvaluator =
                new CustomExpressionEvaluatorTestCase.FooExpressionEvaluator();
        assertTrue("Custom evaluator has not been registered",
                   muleContext.getExpressionManager().isEvaluatorRegistered(customEvaluator.getName()));
    }
}
