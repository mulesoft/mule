/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.el;

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
    protected String getConfigFile()
    {
        return "org/mule/el/expression-language-extension-config.xml";
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
