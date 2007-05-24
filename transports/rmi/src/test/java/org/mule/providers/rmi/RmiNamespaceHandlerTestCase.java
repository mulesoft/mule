/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.rmi;

import org.mule.tck.FunctionalTestCase;

public class RmiNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "rmi-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        RmiConnector c = (RmiConnector) managementContext.getRegistry().lookupConnector("rmiConnector");
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
    }

}
