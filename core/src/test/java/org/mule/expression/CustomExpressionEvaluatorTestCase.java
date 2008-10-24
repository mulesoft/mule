package org.mule.expression;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.expression.ExpressionEvaluatorManager;
import org.mule.util.expression.ExpressionEvaluator;

/**
 * Tests a custom expression evaluator using direct registration with the manager.
 */
public class CustomExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    public void testCustomExpressionEvaluator()
    {
        ExpressionEvaluatorManager.registerEvaluator(new FooExpressionEvaluator());

        Object result = ExpressionEvaluatorManager.evaluate("#[foo:abc]", "test");
        assertNotNull(result);
        assertEquals("Wrong evaluation result", "testabc", result);
    }

    private class FooExpressionEvaluator implements ExpressionEvaluator
    {

        public Object evaluate(String expression, Object message)
        {
            return message + expression;
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
