/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.rmi;

import org.mule.tck.jndi.InMemoryContext;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RmiNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "rmi-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        RmiConnector c = (RmiConnector) muleContext.getRegistry().lookupConnector("rmiConnector");
        assertNotNull(c);
        assertEquals(1234, c.getPollingFrequency());
        assertEquals(DummySecurityManager.class, c.getSecurityManager().getClass());
        String url = c.getSecurityPolicy();
        assertNotNull(url);
        int index = url.lastIndexOf("/");
        assertTrue(index > 0);
        assertEquals("rmi.policy", url.substring(index+1));
        assertEquals("bcd", c.getServerClassName());
        assertEquals("cde", c.getServerCodebase());
        assertEquals("org.mule.tck.jndi.InMemoryContextFactory", c.getJndiInitialFactory());
        assertEquals("efg", c.getJndiProviderUrl());
        assertEquals("fgh", c.getJndiUrlPkgPrefixes());
        assertEquals("hij", c.getJndiProviderProperties().get("ghi"));
    }

    @Test
    public void testConfig2() throws Exception
    {
        RmiConnector c = (RmiConnector) muleContext.getRegistry().lookupConnector("rmiConnector2");
        assertNotNull(c);
        assertEquals(1234, c.getPollingFrequency());
        assertEquals(DummySecurityManager.class, c.getSecurityManager().getClass());
        String url = c.getSecurityPolicy();
        assertNotNull(url);
        int index = url.lastIndexOf("/");
        assertTrue(index > 0);
        assertEquals("rmi.policy", url.substring(index+1));
        assertEquals("bcd", c.getServerClassName());
        assertEquals("cde", c.getServerCodebase());
        assertTrue(c.getJndiContext() instanceof InMemoryContext);
    }

}
