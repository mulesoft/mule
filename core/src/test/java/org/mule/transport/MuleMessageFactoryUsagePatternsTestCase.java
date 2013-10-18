/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MessageRequester;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * This test verifies and illustrates various usage patterns with {@link MuleMessageFactory}. It 
 * uses {@link MessageDispatcher} instances for the test but the same patterns apply to 
 * {@link MessageReceiver} and {@link MessageRequester} as well as all of the code resides in their
 * abstract superclasses.
 */
public class MuleMessageFactoryUsagePatternsTestCase extends AbstractMuleContextTestCase
{
    private OutboundEndpoint endpoint;
    private AbstractConnector connector;
    private MuleMessageFactory factoryFromConnector;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        endpoint = getTestOutboundEndpoint("test");
        connector = (AbstractConnector) endpoint.getConnector();
        factoryFromConnector = connector.getMuleMessageFactory();
    }

    @Test
    public void testSharedMuleMessageFactoryWithConnector() throws Exception
    {
        connector.setDispatcherFactory(new FakeDispatcherFactory());
        
        MockMessageDispatcher dispatcher = 
            (MockMessageDispatcher) connector.getDispatcherFactory().create(endpoint);
        dispatcher.initialise();
        
        MuleMessageFactory factoryFromDispatcher = dispatcher.getMuleMessageFactory();
        assertNotNull(factoryFromDispatcher);
        assertSame(factoryFromConnector, factoryFromDispatcher);
    }
    
    @Test
    public void testMessageDispatcherCreatesOwnMuleMessageFactory() throws Exception
    {
        connector.setDispatcherFactory(new CustomDispatcherFactory());
        
        CustomMessageDispatcher dispatcher = 
            (CustomMessageDispatcher) connector.getDispatcherFactory().create(endpoint);
        dispatcher.initialise();
        
        MuleMessageFactory factoryFromDispatcher = dispatcher.getMuleMessageFactory();
        assertNotNull(factoryFromDispatcher);
        assertNotSame(factoryFromConnector, factoryFromDispatcher);
    }
    
    private static class FakeDispatcherFactory extends AbstractMessageDispatcherFactory
    {
        public FakeDispatcherFactory()
        {
            super();
        }
        
        @Override
        public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
        {
            return new MockMessageDispatcher(endpoint);
        }
    }
    
    private static class CustomDispatcherFactory extends AbstractMessageDispatcherFactory
    {
        public CustomDispatcherFactory()
        {
            super();
        }

        @Override
        public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
        {
            return new CustomMessageDispatcher(endpoint);
        }
    }
    
    private static class MockMessageDispatcher extends AbstractMessageDispatcher
    {
        public MockMessageDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        /**
         * open up access for testing
         */
        public MuleMessageFactory getMuleMessageFactory()
        {
            return muleMessageFactory;
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            throw new UnsupportedOperationException("doDispatch");
        }

        @Override
        protected MuleMessage doSend(MuleEvent event) throws Exception
        {
            throw new UnsupportedOperationException("doSend");
        }
    }
    
    private static class CustomMessageDispatcher extends AbstractMessageDispatcher
    {
        public CustomMessageDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected void initializeMessageFactory() throws InitialisationException
        {
            try
            {
                muleMessageFactory = connector.createMuleMessageFactory();
            }
            catch (CreateException e)
            {
                throw new InitialisationException(e, this);
            }
        }

        /**
         * open up access for testing
         */
        public MuleMessageFactory getMuleMessageFactory()
        {
            return muleMessageFactory;
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            throw new UnsupportedOperationException("doDispatch");
        }

        @Override
        protected MuleMessage doSend(MuleEvent event) throws Exception
        {
            throw new UnsupportedOperationException("doSend");
        }
    }
}
