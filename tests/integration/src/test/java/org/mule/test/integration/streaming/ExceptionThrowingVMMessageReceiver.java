/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.streaming;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.vm.VMMessageReceiver;

public class ExceptionThrowingVMMessageReceiver extends VMMessageReceiver
{

    public ExceptionThrowingVMMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, service, endpoint);
    }

    // @Override
    protected void processMessage(Object msg) throws Exception
    {
        throw new RuntimeException();
    }

    // @Override
    public Object onCall(MuleMessage message, boolean synchronous) throws MuleException
    {
        throw new RuntimeException();
    }

}
