/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.stream;

import org.mule.tck.FunctionalTestCase;

public class SystemStreamNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "system-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        SystemStreamConnector c =
                (SystemStreamConnector)managementContext.getRegistry().lookupConnector("systemConnector");
        assertNotNull(c);

        assertEquals(1234, c.getMessageDelayTime());
        assertEquals("abc", c.getOutputMessage());
        assertEquals("edc", c.getPromptMessage());
        assertEquals("cde", c.getPromptMessageCode());
        assertEquals("dummy", c.getResourceBundle());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    public void testNoBundle() throws Exception
    {
        SystemStreamConnector c =
                (SystemStreamConnector)managementContext.getRegistry().lookupConnector("noBundleConnector");
        assertNotNull(c);

        assertEquals(1234, c.getMessageDelayTime());
        assertEquals("abc", c.getOutputMessage());
        assertEquals("bcd", c.getPromptMessage());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}