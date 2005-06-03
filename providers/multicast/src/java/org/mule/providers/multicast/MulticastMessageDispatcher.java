/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.multicast;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.mule.providers.udp.UdpMessageDispatcher;

/**
 * <code>MulticastMessageDispatcher</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MulticastMessageDispatcher extends UdpMessageDispatcher
{
    public MulticastMessageDispatcher(MulticastConnector connector)
    {
        super(connector);
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

    public void doDispose()
    {
        try {
            ((MulticastSocket) socket).leaveGroup(inetAddress);
        } catch (IOException e) {
            logger.error("Failed to leave group: " + inetAddress);
        }
        super.dispose();
    }
}
