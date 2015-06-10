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
    protected MuleEvent processMessage(Object msg) throws Exception
    {
        throw new RuntimeException();
    }

    @Override
    public MuleMessage onCall(MuleMessage message) throws MuleException
    {
        throw new RuntimeException();
    }

}
