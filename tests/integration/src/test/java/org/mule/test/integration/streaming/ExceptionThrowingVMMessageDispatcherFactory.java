/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.transport.vm.VMMessageDispatcher;

public class ExceptionThrowingVMMessageDispatcherFactory implements MessageDispatcherFactory
{

    public void activate(OutboundEndpoint endpoint, MessageDispatcher dispatcher) throws MuleException
    {
    }

    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new ExceptionThrowingVMMessageDispatcher(endpoint);
    }

    public void destroy(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        // TODO Auto-generated method stub

    }

    public boolean isCreateDispatcherPerRequest()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void passivate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        // TODO Auto-generated method stub

    }

    public boolean validate(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        return true;
    }

    class ExceptionThrowingVMMessageDispatcher extends VMMessageDispatcher
    {

        public ExceptionThrowingVMMessageDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected MuleMessage doSend(MuleEvent event) throws Exception
        {
            throw new RuntimeException("");
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            throw new RuntimeException("");
        }
    }

}
