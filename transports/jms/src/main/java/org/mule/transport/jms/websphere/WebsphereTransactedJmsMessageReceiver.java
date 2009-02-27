/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.websphere;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.jms.XaTransactedJmsMessageReceiver;

import javax.jms.Session;

public class WebsphereTransactedJmsMessageReceiver extends XaTransactedJmsMessageReceiver
{
    public WebsphereTransactedJmsMessageReceiver(Connector connector, Service service, 
        InboundEndpoint endpoint) throws InitialisationException, CreateException
    {
        super(connector, service, endpoint);
    }
    
    protected void doConnect() throws Exception
    {
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


