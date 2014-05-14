/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class MessagePropertyTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/message-properties-transformer-test-case.xml";
    }

    @Test
    public void testAddProperty() throws Exception
    {
        runScenario("addParameter");
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
