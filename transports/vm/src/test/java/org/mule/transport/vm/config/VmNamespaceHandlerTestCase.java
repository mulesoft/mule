/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.config;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.TransactionConfig;
import org.mule.config.QueueProfile;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.vm.VMConnector;


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
        VMConnector c = (VMConnector)muleContext.getRegistry().lookupConnector("vmConnectorDefaults");
        assertNotNull(c);
        
        assertFalse(c.isQueueEvents());
        assertEquals(muleContext.getConfiguration().getDefaultQueueTimeout(), c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNull(queueProfile);
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testDefaultQueueProfile() throws Exception
    {
        VMConnector c = (VMConnector)muleContext.getRegistry().lookupConnector("vmConnector1");
        assertNotNull(c);
        
        assertTrue(c.isQueueEvents());
        assertEquals(muleContext.getConfiguration().getDefaultQueueTimeout(), c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNotNull(queueProfile);
        assertFalse(queueProfile.isPersistent());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testConfig() throws Exception
    {
        VMConnector c = (VMConnector)muleContext.getRegistry().lookupConnector("vmConnector2");
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
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("vmEndpoint");
        assertNotNull(endpoint);
        EndpointURI uri = endpoint.getEndpointURI();
        assertNotNull(uri);
        String address = uri.getAddress();
        assertEquals(address, "queue");
    }
    
    public void testVmTransaction() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("globalWithTx");
        assertNotNull(endpoint);
        
        TransactionConfig txConfig = endpoint.getTransactionConfig();
        assertNotNull(txConfig);
        assertEquals(TransactionConfig.ACTION_ALWAYS_BEGIN, txConfig.getAction());
        assertEquals(42, txConfig.getTimeout());
    }

    public void testCustomTransaction() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointBuilder("customTx").buildInboundEndpoint();
        assertNotNull(endpoint);
        
        TransactionConfig txConfig = endpoint.getTransactionConfig();
        assertNotNull(txConfig);
        assertEquals(TransactionConfig.ACTION_JOIN_IF_POSSIBLE, txConfig.getAction());
        TestTransactionFactory factory = (TestTransactionFactory) endpoint.getTransactionConfig().getFactory();
        assertNotNull(factory);
        assertEquals("foo", factory.getValue());
    }

    public void testXaTransaction() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointBuilder("xaTx").buildInboundEndpoint();
        assertNotNull(endpoint);
        
        TransactionConfig txConfig = endpoint.getTransactionConfig();
        assertNotNull(txConfig);
        assertEquals(TransactionConfig.ACTION_ALWAYS_JOIN, txConfig.getAction());
        assertEquals(XaTransactionFactory.class, txConfig.getFactory().getClass());
    }

}
