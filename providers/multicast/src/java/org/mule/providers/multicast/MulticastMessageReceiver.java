/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.multicast;

import org.mule.InitialisationException;
import org.mule.providers.AbstractConnector;
import org.mule.providers.udp.UdpMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;

/**
 * <code>MulticastMessageReceiver</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MulticastMessageReceiver extends UdpMessageReceiver
{
    public MulticastMessageReceiver(AbstractConnector connector,
                              UMOComponent component,
                              UMOEndpoint endpoint) throws InitialisationException
    {
        super(connector, component, endpoint);
    }

    protected DatagramSocket createSocket(URI uri, InetAddress inetAddress) throws IOException
    {
        //SocketAddress sa = new InetSocketAddress(uri.getHost(), uri.getPort());
        MulticastSocket socket = new MulticastSocket(uri.getPort());
        socket.joinGroup(inetAddress);
        return socket;
    }

    protected Runnable createWorker(DatagramPacket packet) throws IOException
    {
        return new UdpWorker(socket, packet);
    }

    public void doDispose() throws UMOException
    {
        try
        {
            ((MulticastSocket)socket).leaveGroup(inetAddress);
        } catch (IOException e)
        {
            logger.error("failed to leave group: " + e.getMessage(), e);
        }
    }
}