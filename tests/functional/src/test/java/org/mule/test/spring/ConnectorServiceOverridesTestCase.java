/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import org.mule.MessageExchangePattern;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestMessageReceiver;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.service.TransportServiceDescriptor;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConnectorServiceOverridesTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/service-overrides.xml";
    }

    @Test
    public void testOverrideMessageReceiver() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        
        // create an xa-transacted endpoint (this triggers the cration of an 
        // xaTransactedMessageReceiver in the service descriptor impl
        InboundEndpoint endpoint = getTestInboundEndpoint("foo");
        endpoint.getTransactionConfig().setAction(MuleTransactionConfig.ACTION_ALWAYS_BEGIN);
        endpoint.getTransactionConfig().setFactory(new XaTransactionFactory());
        
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();

        // see if we get the overridden message receiver
        MessageReceiver receiver = serviceDescriptor.createMessageReceiver(connector, 
            getTestService(), endpoint);
        assertEquals(TestMessageReceiver.class, receiver.getClass());        
    }

    private TestConnector lookupDummyConnector()
    {
        TestConnector connector = (TestConnector) muleContext.getRegistry().lookupConnector("dummyConnector");
        assertNotNull(connector);
        return connector;
    }
    
    @Test
    public void testOverrideMuleMessageFactory() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();

        // test if the service override for the message factory works
        MuleMessageFactory messageFactory = serviceDescriptor.createMuleMessageFactory();
        assertEquals(MockMuleMessageFactory.class, messageFactory.getClass());
    }

    @Test
    public void testOverrideInbounExchangePatterns() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();
        
        List<MessageExchangePattern> meps = serviceDescriptor.getInboundExchangePatterns();
        
        List<MessageExchangePattern> expected = Arrays.asList(MessageExchangePattern.REQUEST_RESPONSE);
        assertEquals(expected, meps);
    }
    
    @Test
    public void testOverrideOutboundExchangePatterns() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();
        
        List<MessageExchangePattern> meps = serviceDescriptor.getOutboundExchangePatterns();
        
        List<MessageExchangePattern> expected = Arrays.asList(MessageExchangePattern.REQUEST_RESPONSE);
        assertEquals(expected, meps);
    }

    @Test
    public void testOverrideDefaultExchangePattern() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();
        
        MessageExchangePattern defaultMep = serviceDescriptor.getDefaultExchangePattern();
        
        assertEquals(MessageExchangePattern.REQUEST_RESPONSE, defaultMep);
    }
}


