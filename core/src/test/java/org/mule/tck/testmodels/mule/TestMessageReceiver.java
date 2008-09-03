/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;

public class TestMessageReceiver extends AbstractMessageReceiver
{

    public TestMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint) throws CreateException
    {
        super(connector, service, endpoint);
    }

    protected void doConnect() throws Exception
    {
        // no op
    }

    protected void doDisconnect() throws Exception
    {
        // no op
    }

    protected void doInitialise()
    {
        // template method
    }

    protected void doDispose()
    {
        // no op
    }

    protected void doStart() throws MuleException
    {
        // no op
    }

    protected void doStop() throws MuleException
    {
        // no op
    }

}


