/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import org.mule.api.MuleEvent;

import org.junit.Test;

public class GroovyScriptFlowFunctionalTestCase extends GroovyScriptServiceFunctionalTestCase
{

    @Test
    public void inlineScriptMutateProperty() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.getMessage().setOutboundProperty("foo", "bar");
        testFlow("inlineScriptMutateProperty", event);
    }

    @Test
    public void inlineScriptAddProperty() throws Exception
    {
        MuleEvent event = getTestEvent("");
        testFlow("inlineScriptMutateProperty", event);
    }

    @Test
    public void inlineScriptMutatePropertiesMap() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.getMessage().setOutboundProperty("foo", "bar");
        testFlow("inlineScriptMutatePropertiesMap", event);
    }

    @Test
    public void inlineScriptMutateVariable() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.getMessage().setInvocationProperty("foo", "bar");
        testFlow("inlineScriptMutateVariable", event);
    }

    @Test
    public void inlineScriptAddVariable() throws Exception
    {
        MuleEvent event = getTestEvent("");
        testFlow("inlineScriptAddVariable", event);
    }

    @Test
    public void inlineScriptMutateVariablesMap() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.getMessage().setInvocationProperty("foo", "bar");
        testFlow("inlineScriptMutateVariablesMap", event);
    }

    @Test
    public void inlineScriptMutatePayload() throws Exception
    {
        MuleEvent event = getTestEvent("");
        testFlow("inlineScriptMutatePayload", event);
    }

    @Test
    public void scriptExpressionVariables() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("prop1", "Received");
        event.setFlowVariable("prop2", "A-OK");
        testFlow("scriptExpressionVariables", event);
    }

    @Override
    protected String getConfigFile()
    {
        return "groovy-component-config-flow.xml";
    }
}
