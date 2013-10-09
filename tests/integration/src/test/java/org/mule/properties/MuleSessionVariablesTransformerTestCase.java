/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.properties;

import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

public class MuleSessionVariablesTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());
        Flow flow = (Flow) getFlowConstruct(flowName);
        flow.process(event);
        FlowAssert.verify(flowName);
    }

}
