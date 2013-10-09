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

public class MessagePropertyTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/properties/message-properties-transformer-test-case.xml";
    }

    @Test
    public void testAddProperty() throws Exception
    {
        runScenario("addProperty");
    }
    
    @Test
    public void testAddPropertyWithExpressionKey() throws Exception
    {
        runScenario("addPropertyUsingExpressionKey");
    }

    @Test
    public void testRemoveProperty() throws Exception
    {
        runScenario("removeProperty");
    }

    @Test
    public void testRemovePropertyUsingExpression() throws Exception
    {
        runScenario("removePropertyUsingExpression");
    }

    @Test
    public void testRemovePropertyUsingRegex() throws Exception
    {
        runScenario("removePropertyUsingRegex");
    }

    @Test
    public void testRemoveAllProperties() throws Exception
    {
        runScenario("removeAllProperties");
    }

    @Test
    public void testCopyProperties() throws Exception
    {
        runScenario("copyProperties");
    }

    @Test
    public void testCopyPropertiesUsingExpression() throws Exception
    {
        runScenario("copyPropertiesUsingExpression");
    }

    @Test
    public void testCopyAllProperties() throws Exception
    {
        runScenario("copyAllProperties");
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
