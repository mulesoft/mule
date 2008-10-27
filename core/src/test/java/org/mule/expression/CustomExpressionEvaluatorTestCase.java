package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.transport.MessageAdapter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.expression.ExpressionEvaluator;
import org.mule.util.expression.ExpressionEvaluatorManager;

/**
 * Tests a custom expression evaluator using direct registration with the manager.
 */
public class CustomExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    public void testCustomExpressionEvaluator()
    {
        ExpressionEvaluatorManager.registerEvaluator(new FooExpressionEvaluator());

        Object result = ExpressionEvaluatorManager.evaluate("#[foo:abc]", new DefaultMuleMessage("test"));
        assertNotNull(result);
        assertEquals("Wrong evaluation result", "testabc", result);
    }

    private class FooExpressionEvaluator implements ExpressionEvaluator
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
