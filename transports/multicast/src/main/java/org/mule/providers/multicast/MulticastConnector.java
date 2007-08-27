/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.providers.udp.UdpConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>MulticastConnector</code> can dispatch mule events using ip multicasting
 */
public class MulticastConnector extends UdpConnector
{
    private boolean loopback = false;
    private int timeToLive = INT_VALUE_NOT_SET;

    public String getProtocol()
    {
        return "multicast";
    }


    //@java.lang.Override
    protected void doInitialise() throws InitialisationException
    {
        dispatcherSocketsPool.setFactory(new MulticastSocketFactory());
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


    //@java.lang.Override
    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        //you can have multiple Multicast sockets bound to a single port,
        // so store listeners with the component name too
        return endpoint.getEndpointURI().getAddress() + "/" + component.getDescriptor().getName();
    }
}
