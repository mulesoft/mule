/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestMessageReceiver;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.DefaultMuleMessageFactory;
import org.mule.transport.service.TransportServiceDescriptor;

import java.util.Arrays;
import java.util.List;

public class ConnectorServiceOverridesTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/service-overrides.xml";
    }

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
    
    public void testOverrideMuleMessageFactory() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();

        // test if the service override for the message factory works
        MuleMessageFactory messageFactory = serviceDescriptor.createMuleMessageFactory();
        assertEquals(MockMuleMessageFactory.class, messageFactory.getClass());
    }

    public void testOverrideInbounExchangePatterns() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();
        
        List<MessageExchangePattern> meps = serviceDescriptor.getInboundExchangePatterns();
        
        List<MessageExchangePattern> expected = Arrays.asList(MessageExchangePattern.REQUEST_RESPONSE);
        assertEquals(expected, meps);
    }
    
    public void testOverrideOutboundExchangePatterns() throws Exception
    {
        TestConnector connector = lookupDummyConnector();
        TransportServiceDescriptor serviceDescriptor = connector.getServiceDescriptor();
        
        List<MessageExchangePattern> meps = serviceDescriptor.getOutboundExchangePatterns();
        
        List<MessageExchangePattern> expected = Arrays.asList(MessageExchangePattern.REQUEST_RESPONSE);
        assertEquals(expected, meps);
    }

    public static class MockMuleMessageFactory extends DefaultMuleMessageFactory
    {
        public MockMuleMessageFactory(MuleContext context)
        {
            super(context);
        }
    }
}


