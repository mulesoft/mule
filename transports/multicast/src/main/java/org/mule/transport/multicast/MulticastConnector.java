/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.multicast;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.udp.UdpConnector;

/**
 * <code>MulticastConnector</code> can dispatch mule events using ip multicasting
 */
public class MulticastConnector extends UdpConnector
{

    public static final String MULTICAST = "multicast";
    private boolean loopback = false;
    private int timeToLive = INT_VALUE_NOT_SET;

    public String getProtocol()
    {
        return MULTICAST;
    }

    public MulticastConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        socketFactory = new MulticastSocketFactory();
        dispatcherSocketsPool.setFactory(socketFactory);
        dispatcherSocketsPool.setTestOnBorrow(false);
        dispatcherSocketsPool.setTestOnReturn(true);
        //For clarity, note that the max active value does not need to be 1 since you can have multiple
        //Multicast sockets bound to a single port
        //dispatcherSocketsPool.setMaxActive(1);
    }

    public boolean isLoopback()
    {
        return loopback;
    }

    public void setLoopback(boolean loopback)
    {
        this.loopback = loopback;
    }


    public int getTimeToLive()
    {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive)
    {
        this.timeToLive = timeToLive;
    }


    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        //you can have multiple Multicast sockets bound to a single port,
        // so store listeners with the service name too
        return endpoint.getEndpointURI().getAddress() + "/" + flowConstruct.getName();
    }
}
