/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
