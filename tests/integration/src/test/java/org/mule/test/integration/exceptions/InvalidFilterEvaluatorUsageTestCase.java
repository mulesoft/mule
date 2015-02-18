/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.expression.ExceptionTypeExpressionEvaluator;
import org.mule.expression.PayloadTypeExpressionEvaluator;
import org.mule.expression.RegexExpressionEvaluator;
import org.mule.expression.WilcardExpressionEvaluator;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
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
    protected String getConfigFile()
    {
        return "invalid-filter-evaluator-usage-config.xml";
    }

    @Test
    public void producesClearErrorMessage() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        final HttpRequestOptions httpRequestOptions = HttpRequestOptionsBuilder.newOptions().disableStatusCodeValidation().build();
        MuleMessage response = client.send("http://localhost:" + port.getNumber(), getTestMuleMessage(), httpRequestOptions);
        assertTrue(response.getPayloadAsString().contains(evaluatorName));
        assertTrue(response.getPayloadAsString().contains("java.lang.UnsupportedOperationException"));
    }
}
