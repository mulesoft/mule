/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.websphere;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.transport.jms.XaTransactedJmsMessageReceiver;

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


