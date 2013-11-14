/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class GroovyContextTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "groovy-context-config.xml";
    }

    @Test
    public void sessionVariable() throws Exception
    {
        runScenario("sessionVariable");
    }

    @Test
    public void flowVariable() throws Exception
    {
        runScenario("flowVariable");
    }

    @Test
    public void flowShadowsSessionVariable() throws Exception
    {
        runScenario("flowShadowsSessionVariable");
    }

    @Test
    @Ignore(value = "See MULE-6210")
    public void noVariable() throws Exception
    {
        runScenario("noVariable");
    }

    @Test
    public void flowVars() throws Exception
    {
        runScenario("flowVars");
    }

    @Test
    public void sessionVars() throws Exception
    {
        runScenario("sessionVars");
    }

    @Test
    public void flowVar() throws Exception
    {
        runScenario("flowVar");
    }

    @Test
    public void emptyFlowVar() throws Exception
    {
        runScenario("emptyFlowVar");
    }

    @Test
    public void noFlowVar() throws Exception
    {
        runScenario("noFlowVar");
    }

    @Test
    public void sessionVar() throws Exception
    {
        runScenario("sessionVar");
    }

    @Test
    public void emptySessionVar() throws Exception
    {
        runScenario("emptySessionVar");
    }

    @Test
    public void noSessionVar() throws Exception
    {
        runScenario("noSessionVar");
    }


    @Test
    public void exception() throws Exception
    {
        runScenario("exceptionFlow");
    }

    @Test
    public void noException() throws Exception
    {
        runScenario("noException");
    }

    @Test
    public void flowVarLegacy() throws Exception
    {
        runScenario("flowVarLegacy");
    }

    @Test
    @Ignore(value = "See MULE-6211")
    public void exceptionFlowLegacy() throws Exception
    {
        runScenario("exceptionFlowLegacy");
    }

    protected void runScenario(String flowName) throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("TEST", muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());
        Flow flow = (Flow) getFlowConstruct(flowName);
        flow.process(event);
        FlowAssert.verify(flowName);
    }
}
