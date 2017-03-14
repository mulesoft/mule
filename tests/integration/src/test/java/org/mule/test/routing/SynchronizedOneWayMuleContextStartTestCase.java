/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.transport.vm.VMMessageDispatcher;

public class SynchronizedOneWayMuleContextStartTestCase extends SynchronizedMuleContextStartTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "synchronized-one-way-mule-context-start-config.xml";
    }

    public static class StartCheckVmMessageDispatcherFactory implements MessageDispatcherFactory
    {

        public void activate(OutboundEndpoint endpoint, MessageDispatcher dispatcher) throws MuleException
        {
        }

        public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
        {
            return new StartCheckVMMessageDispatcher(endpoint);
        }

        public void destroy(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
        {

        }

        public boolean isCreateDispatcherPerRequest()
        {
            return false;
        }

        public void passivate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
        {
        }

        public boolean validate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
        {
            return true;
        }

        class StartCheckVMMessageDispatcher extends VMMessageDispatcher
        {

            private final VMMessageDispatcher delegate;

            public StartCheckVMMessageDispatcher(OutboundEndpoint endpoint)
            {
                super(endpoint);
                delegate = new VMMessageDispatcher(endpoint);
            }

            @Override
            protected MuleMessage doSend(MuleEvent event) throws Exception
            {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void doDispatch(MuleEvent event) throws Exception
            {
                if (!muleContext.isStarted())
                {
                    throw new IllegalStateException("Mule context is not started yet");
                }
                else
                {
                    processedMessageCounter++;
                }
                delegate.process(event);
            }
        }
    }
}
