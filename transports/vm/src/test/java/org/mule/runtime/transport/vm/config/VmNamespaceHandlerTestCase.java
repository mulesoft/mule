/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.config.QueueProfile;
import org.mule.runtime.core.transaction.XaTransactionFactory;
import org.mule.runtime.transport.vm.VMConnector;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Test;


/**
 * Tests the Spring XML namespace for the VM transport.
 */
public class VmNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "vm/vm-namespace-config.xml";
    }

    @Test
    public void testDefaults() throws Exception
    {
        VMConnector c = (VMConnector)muleContext.getRegistry().lookupConnector("vmConnectorDefaults");
        assertNotNull(c);
        
        assertEquals(muleContext.getConfiguration().getDefaultQueueTimeout(), c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNotNull(queueProfile);
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    @Test
    public void testDefaultQueueProfile() throws Exception
    {
        VMConnector c = (VMConnector)muleContext.getRegistry().lookupConnector("vmConnector1");
        assertNotNull(c);
        
        assertEquals(muleContext.getConfiguration().getDefaultQueueTimeout(), c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNotNull(queueProfile);
        //assertFalse(queueProfile.isPersistent());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    @Test
    public void testConfig() throws Exception
    {
        VMConnector c = (VMConnector)muleContext.getRegistry().lookupConnector("vmConnector2");
        assertNotNull(c);
        
        assertEquals(5000, c.getQueueTimeout());
        QueueProfile queueProfile = c.getQueueProfile();
        assertNotNull(queueProfile);
        //assertTrue(queueProfile.isPersistent());
        assertEquals(10, queueProfile.getMaxOutstandingMessages());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testGlobalEndpoint() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("vmEndpoint");
        assertNotNull(endpoint);
        EndpointURI uri = endpoint.getEndpointURI();
        assertNotNull(uri);
        String address = uri.getAddress();
        assertEquals(address, "queue");
    }
    
    @Test
    public void testVmTransaction() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("globalWithTx");
        assertNotNull(endpoint);
        
        TransactionConfig txConfig = endpoint.getTransactionConfig();
        assertNotNull(txConfig);
        assertEquals(TransactionConfig.ACTION_ALWAYS_BEGIN, txConfig.getAction());
        assertEquals(42, txConfig.getTimeout());
    }

    @Test
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

    @Test
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
