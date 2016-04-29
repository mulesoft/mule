/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.websphere;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.transport.jms.XaTransactedJmsMessageReceiver;

import javax.jms.Session;

public class WebsphereTransactedJmsMessageReceiver extends XaTransactedJmsMessageReceiver
{
    public WebsphereTransactedJmsMessageReceiver(Connector connector, FlowConstruct flowConstruct,
        InboundEndpoint endpoint) throws InitialisationException, CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void doConnect() throws Exception
    {
        super.doConnect();

        if (connector.isConnected() && connector.isEagerConsumer())
        {
            createConsumer();
        }

        // TODO make it configurable. This connection blip is killing performance with WMQ, session create() and close()
        // are the heaviest operations there, synchronizing on a global QM for this machine
        // MULE-1150 check whether mule is really connected
        if (connector.isConnected() && !this.connected.get() && connector.getSessionFromTransaction() == null)
        {
            // check connection by creating session
            Session s = connector.getConnection().createSession(false, 1);
            s.close();
        }
    }
}


