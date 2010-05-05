/*
 * $Id:SpringNamespaceConfigBuilderTestCase.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestMessageReceiver;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.DefaultMuleMessageFactory;
import org.mule.transport.service.TransportServiceDescriptor;

public class SpringNamespaceConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{
    public SpringNamespaceConfigBuilderTestCase()
    {
        super(false);
    }

    @Override
    public String getConfigResources()
    {
        return "org/mule/test/spring/config1/test-xml-mule2-config.xml," +
                "org/mule/test/spring/config1/test-xml-mule2-config-split.xml";
    }

    public void testServiceOverrides() throws Exception
    {
        TestConnector connector = (TestConnector) muleContext.getRegistry().lookupConnector("dummyConnector");
        assertNotNull(connector);
        
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
        
        // test if the service override for the message factory works
        MuleMessageFactory messageFactory = serviceDescriptor.createMuleMessageFactory();
        assertEquals(MockMuleMessageFactory.class, messageFactory.getClass());
    }
    
    public static class MockMuleMessageFactory extends DefaultMuleMessageFactory
    {
        public MockMuleMessageFactory(MuleContext context)
        {
            super(context);
        }
    }
}
