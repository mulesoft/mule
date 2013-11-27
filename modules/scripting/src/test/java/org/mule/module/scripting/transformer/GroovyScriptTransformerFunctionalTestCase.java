/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class GroovyScriptTransformerFunctionalTestCase extends FunctionalTestCase
{

    public GroovyScriptTransformerFunctionalTestCase()
    {
        // Groovy really hammers the startup time since it needs to create the interpreter on every start
        setDisposeContextPerClass(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "groovy-transformer-config-flow.xml";
    }

    @Test
    public void testInlineScript() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in1", "hello", null);
        MuleMessage response = client.request("vm://out1", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }

    @Test
    public void testFileBasedScript() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in2", "hello", null);
        MuleMessage response = client.request("vm://out2", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }

    @Test
    public void testReferencedTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in3", "hello", null);
        MuleMessage response = client.request("vm://out3", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }

    @Test
    public void testReferencedTransformerWithParameters() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in4", "hello", null);
        MuleMessage response = client.request("vm://out4", RECEIVE_TIMEOUT);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }

    @Test
    public void transformByAssigningPayload() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in5", "hello", null);
        assertNotNull(response);
        assertEquals("bar", response.getPayload());
    }

    @Test
    public void transformByAssigningHeader() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in6", "hello", null);
        assertNotNull(response);
        assertEquals("hello", response.getPayload());
        assertEquals("bar", response.getInboundProperty("foo"));
    }
}
