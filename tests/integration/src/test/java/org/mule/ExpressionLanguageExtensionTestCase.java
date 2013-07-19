package org.mule;

import static org.junit.Assert.assertSame;
import org.mule.api.MuleContext;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.el.ExpressionLanguage;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.expression.DefaultExpressionManager;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ExpressionLanguageExtensionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "expression-language-extension-config.xml";
    }

    @Test
    public void doesNotOverrideExpressionLanguageInExpressionManagerOnCreation() throws Exception
    {
        ExpressionLanguage originalExpressionLanguage = ((DefaultExpressionManager) muleContext.getExpressionManager()).getExpressionLanguage();

        LocalMuleClient client = muleContext.getClient();
        client.send("vm://testInput", TEST_MESSAGE, null);

        ExpressionLanguage newExpressionLanguage = ((DefaultExpressionManager) muleContext.getExpressionManager()).getExpressionLanguage();

        assertSame(originalExpressionLanguage, newExpressionLanguage);
    }

    public static class ExpressionLanguageFactory
    {
        public Object process(Object value)
        {
            new TestExpressionLanguage(muleContext);

            return value;
        }
    }

    public static class TestExpressionLanguage extends MVELExpressionLanguage
    {

        public TestExpressionLanguage(MuleContext muleContext)
        {
            super(muleContext);
        }
    }
}
