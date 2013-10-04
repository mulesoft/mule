/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.stdio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class StdioNamespaceHandlerTestCase extends AbstractServiceAndFlowTestCase
{

    public StdioNamespaceHandlerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "stdio-namespace-config-service.xml"},
            {ConfigVariant.FLOW, "stdio-namespace-config-flow.xml"}
        });
    }
    
    @Test
    public void testConfig() throws Exception
    {
        PromptStdioConnector c =
                (PromptStdioConnector) muleContext.getRegistry().lookupConnector("stdioConnector");
        assertNotNull(c);

        assertEquals(1234, c.getMessageDelayTime());
        assertEquals("abc", c.getOutputMessage());
        assertEquals("edc", c.getPromptMessage());
        assertEquals("456", c.getPromptMessageCode());
        assertEquals("dummy-messages", c.getResourceBundle());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testNoBundle() throws Exception
    {
        PromptStdioConnector c =
                (PromptStdioConnector)muleContext.getRegistry().lookupConnector("noBundleConnector");
        assertNotNull(c);

        assertEquals(1234, c.getMessageDelayTime());
        assertEquals("abc", c.getOutputMessage());
        assertEquals("bcd", c.getPromptMessage());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testSystemAttributeMap()
    {
        testEndpointAttribute("in", "system.in");
        testEndpointAttribute("out", "system.out");
        testEndpointAttribute("err", "system.err");
    }

    protected void testEndpointAttribute(String name, String address)
    {
        ImmutableEndpoint endpoint = (ImmutableEndpoint) muleContext.getRegistry().lookupObject(name);
        assertNotNull("Null " + name, endpoint);
        assertEquals(address, endpoint.getEndpointURI().getAddress());
    }

}
