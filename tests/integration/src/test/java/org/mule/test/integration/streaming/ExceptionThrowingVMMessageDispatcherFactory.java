/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
