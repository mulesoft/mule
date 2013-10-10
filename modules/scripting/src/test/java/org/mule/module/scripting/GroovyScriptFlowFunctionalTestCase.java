/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    @Override
    protected String getConfigResources()
    {
        return "groovy-component-config-flow.xml";
    }

}
