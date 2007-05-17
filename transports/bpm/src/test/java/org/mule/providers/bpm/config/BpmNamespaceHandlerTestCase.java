/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.bpm.config;

import org.mule.providers.bpm.BPMS;
import org.mule.providers.bpm.ProcessConnector;
import org.mule.providers.bpm.test.TestBpms;
import org.mule.tck.FunctionalTestCase;


/**
 * Tests the Spring XML namespace for the BPM transport.
 */
public class BpmNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "bpm-namespace-config.xml";
    }

    public void testDefaults() throws Exception
    {
        ProcessConnector c = (ProcessConnector)managementContext.getRegistry().lookupConnector("bpmConnectorDefaults");
        assertNotNull(c);
        
        assertFalse(c.isAllowGlobalDispatcher());
        assertFalse(c.isAllowGlobalReceiver());
        assertNull(c.getProcessIdField());
        
        BPMS bpms = c.getBpms();
        assertNotNull(bpms);
        assertEquals(TestBpms.class, bpms.getClass());
        assertEquals("bar", ((TestBpms) bpms).getFoo());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testConfig() throws Exception
    {
        ProcessConnector c = (ProcessConnector)managementContext.getRegistry().lookupConnector("bpmConnector1");
        assertNotNull(c);
        
        assertTrue(c.isAllowGlobalDispatcher());
        assertTrue(c.isAllowGlobalReceiver());
        assertEquals("myId", c.getProcessIdField());
        
        BPMS bpms = c.getBpms();
        assertNotNull(bpms);
        assertEquals(TestBpms.class, bpms.getClass());
        assertEquals("bar", ((TestBpms) bpms).getFoo());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }    
}
