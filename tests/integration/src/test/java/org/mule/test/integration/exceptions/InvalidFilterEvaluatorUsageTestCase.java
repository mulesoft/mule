/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.expression.ExceptionTypeExpressionEvaluator;
import org.mule.expression.PayloadTypeExpressionEvaluator;
import org.mule.expression.RegexExpressionEvaluator;
import org.mule.expression.WilcardExpressionEvaluator;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InvalidFilterEvaluatorUsageTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port1");

    @Rule
    public SystemProperty expression;

    private final String evaluatorName;

    public InvalidFilterEvaluatorUsageTestCase(String evaluatorName) throws Throwable
    {
        this.evaluatorName = evaluatorName;

        expression = new SystemProperty("expression", evaluatorName + ":dummy expression");
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {PayloadTypeExpressionEvaluator.NAME},
                {ExceptionTypeExpressionEvaluator.NAME},
                {RegexExpressionEvaluator.NAME},
                {WilcardExpressionEvaluator.NAME}
        });
    }

    @Override
    protected String getConfigResources()
    {
        return "invalid-filter-evaluator-usage-config.xml";
    }

    @Test
    public void producesClearErrorMessage() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + port.getNumber(), "TEST", null);
        assertTrue(response.getPayloadAsString().contains(evaluatorName));
        assertTrue(response.getPayloadAsString().contains("java.lang.UnsupportedOperationException"));
    }
}
