/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class MuleSessionVariablesTransformerTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/mule-session-variables-transformer-test-case.xml";
    }

    @Test
    public void testAddVariable() throws Exception
    {
        runScenario("addSessionVariableFlow");
    }

    @Test
    public void testAddVariableWithExpressionKey() throws Exception
    {
        runScenario("addSessionVariableUsingExpressionKeyFlow");
    }

    @Test
    public void testAddVariableWithParsedStringKey() throws Exception
    {
        runScenario("addVariableWithParsedStringKeyFlow");
    }
    
    @Test
    public void testRemoveVariable() throws Exception
    {
        runScenario("removeSessionVariableFlow");
    }

    @Test
    public void testRemoveVariableUsingExpression() throws Exception
    {
        runScenario("removeSessionVariableUsingExpressionFlow");
    }

    @Test
    public void testRemoveVariableUsingParsedString() throws Exception
    {
        runScenario("removeSessionVariableUsingParsedStringFlow");
    }

    @Test
    public void testRemoveVariableUsingRegex() throws Exception
    {
        runScenario("removeSessionVariableUsingRegexFlow");
    }

    @Test
    public void testRemoveAllVariables() throws Exception
    {
        runScenario("removeAllSessionVariablesFlow");
    }

    public void runScenario(String flowName) throws Exception
    {
        flowRunner(flowName).withPayload("data").run();
    }
}
