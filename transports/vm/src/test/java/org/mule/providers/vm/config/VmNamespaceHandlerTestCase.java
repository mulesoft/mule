/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.vm.config;

import org.mule.config.QueueProfile;
import org.mule.providers.vm.VMConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;


/**
 * Tests the Spring XML namespace for the VM transport.
 */
public class VmNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "vm/vm-namespace-config.xml";
    }

    public void testDefaults() throws Exception
    {
        VMConnector c = (VMConnector)managementContext.getRegistry().lookupConnector("vmConnectorDefaults");
        assertNotNull(c);
        
        assertFalse(c.isQueueEvents());
        assertEquals(1000, c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNull(queueProfile);
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testDefaultQueueProfile() throws Exception
    {
        VMConnector c = (VMConnector)managementContext.getRegistry().lookupConnector("vmConnector1");
        assertNotNull(c);
        
        assertTrue(c.isQueueEvents());
        assertEquals(1000, c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNotNull(queueProfile);
        assertFalse(queueProfile.isPersistent());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testConfig() throws Exception
    {
        VMConnector c = (VMConnector)managementContext.getRegistry().lookupConnector("vmConnector2");
        assertNotNull(c);
        
        assertTrue(c.isQueueEvents());
        assertEquals(5000, c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNotNull(queueProfile);
        assertTrue(queueProfile.isPersistent());
        assertEquals(10, queueProfile.getMaxOutstandingMessages());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    public void testGlobalEndpoint() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("vmEndpoint");
        assertNotNull(endpoint);
        UMOEndpointURI uri = endpoint.getEndpointURI();
        assertNotNull(uri);
        String address = uri.getAddress();
        assertEquals(address, "queue");
    }

}
