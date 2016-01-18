/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class GroovyScriptServiceFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "groovy-component-config.xml";
    }

    @Test
    public void testInlineScript() throws Exception
    {
        MuleClient client = muleContext.getClient();
        runFlowAsync("inlineScript", "Important Message");
        MuleMessage response = client.request("test://inlineScriptTestOut", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received", getPayloadAsString(response));
    }

    @Ignore("MULE-6926: flaky test")
    @Test
    public void testFileBasedScript() throws Exception
    {
        MuleClient client = muleContext.getClient();
        runFlowAsync("fileBasedScript", "Important Message");
        MuleMessage response = client.request("test://fileBasedScriptTestOut", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received", getPayloadAsString(response));
    }

    @Test
    public void testReferencedScript() throws Exception
    {
        MuleClient client = muleContext.getClient();
        runFlowAsync("referencedScript", "Important Message");
        MuleMessage response = client.request("test://referencedScriptTestOut", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received", getPayloadAsString(response));
    }

    @Ignore("MULE-6926: flaky test")
    @Test
    public void testScriptVariables() throws Exception
    {
        MuleClient client = muleContext.getClient();
        runFlowAsync("scriptVariables", "Important Message");
        MuleMessage response = client.request("test://scriptVariablesTestOut", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received A-OK", getPayloadAsString(response));
    }
}
