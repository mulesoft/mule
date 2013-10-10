/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.config;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.TransactionConfig;
import org.mule.config.QueueProfile;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.vm.VMConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Tests the Spring XML namespace for the VM transport.
 */
public class VmNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
