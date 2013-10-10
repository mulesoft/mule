/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class GroovyScriptServiceFunctionalTestCase extends FunctionalTestCase
{

    @Test
    public void testInlineScript() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in1", "Important Message", null);
        MuleMessage response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received", response.getPayloadAsString());
    }

    @Test
    public void testFileBasedScript() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in2", "Important Message", null);
        MuleMessage response = client.request("vm://out2", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received", response.getPayloadAsString());
    }

    @Test
    public void testReferencedScript() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in3", "Important Message", null);
        MuleMessage response = client.request("vm://out3", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received", response.getPayloadAsString());
    }

    @Test
    public void testScriptVariables() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in4", "Important Message", null);
        MuleMessage response = client.request("vm://out4", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("Important Message Received A-OK", response.getPayloadAsString());
    }

    @Override
    protected String getConfigResources()
    {
        return "groovy-component-config-service.xml";
    }
}
