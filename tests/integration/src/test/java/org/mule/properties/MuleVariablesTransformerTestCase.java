/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class MuleVariablesTransformerTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/mule-variables-transformer-test-case.xml";
    }

    @Test
    public void testAddVariable() throws Exception
    {
        runScenario("addVariable");
    }

    @Test
    public void testAddVariableWithExpressionKey() throws Exception
    {
        runScenario("addVariableUsingExpressionKey");
    }

    @Test
    public void testRemoveVariable() throws Exception
    {
        runScenario("removeVariable");
    }

    @Test
    public void testRemoveVariableUsingExpression() throws Exception
    {
        runScenario("removeVariableUsingExpression");
    }

    @Test
    public void testRemoveVariableUsingRegex() throws Exception
    {
        runScenario("removeVariableUsingRegex");
    }

    @Test
    public void testRemoveAllVariables() throws Exception
    {
        runScenario("removeAllVariables");
    }

    public void runScenario(String flowName) throws Exception
    {
        flowRunner(flowName).withPayload("data").run();
    }

}
