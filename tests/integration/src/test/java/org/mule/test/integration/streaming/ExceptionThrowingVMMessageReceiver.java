/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.streaming;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.vm.VMMessageReceiver;

public class ExceptionThrowingVMMessageReceiver extends VMMessageReceiver
{

    public ExceptionThrowingVMMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void processMessage(Object msg) throws Exception
    {
        throw new RuntimeException();
    }

    @Override
    public MuleMessage onCall(MuleMessage message) throws MuleException
    {
        throw new RuntimeException();
    }

}
