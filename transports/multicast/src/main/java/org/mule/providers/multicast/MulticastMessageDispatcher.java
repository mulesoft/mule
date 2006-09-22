/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.multicast;

import org.mule.providers.udp.UdpMessageDispatcher;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * <code>MulticastMessageDispatcher</code> dispatches events to a multicast address
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MulticastMessageDispatcher extends UdpMessageDispatcher
{
    public MulticastMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected DatagramSocket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        MulticastSocket socket = new MulticastSocket(port);
        socket.setLoopbackMode(((MulticastConnector) connector).isLoopback());
        // socket.setBroadcast(connector.getBroadcast());
        socket.setReceiveBufferSize(connector.getBufferSize());
        socket.setSendBufferSize(connector.getBufferSize());
        socket.joinGroup(inetAddress);
        return socket;
    }

    protected void doDisconnect() throws Exception {
        try {
            if(socket!=null) {
                ((MulticastSocket) socket).leaveGroup(inetAddress);
            }
        } catch (IOException e) {
            logger.error("Failed to leave group: " + inetAddress);
        }
        super.doDisconnect();
    }
}
