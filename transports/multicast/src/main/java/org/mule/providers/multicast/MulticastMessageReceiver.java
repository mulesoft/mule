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

import org.mule.providers.AbstractConnector;
import org.mule.providers.udp.UdpMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;

import javax.resource.spi.work.Work;

public class MulticastMessageReceiver extends UdpMessageReceiver
{

    public MulticastMessageReceiver(AbstractConnector connector, UMOComponent component, UMOImmutableEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
    }

    protected DatagramSocket createSocket(URI uri, InetAddress inetAddress) throws IOException
    {
        MulticastSocket socket = new MulticastSocket(uri.getPort());
        socket.joinGroup(inetAddress);
        return socket;
    }

    protected Work createWork(DatagramPacket packet) throws IOException
    {
        return new MulticastWorker(packet);
    }

    public class MulticastWorker extends UdpWorker
    {
        public MulticastWorker(DatagramPacket packet)
        {
            super(socket, packet);
        }

        public void dispose()
        {
            // Do not close socket as we reuse it
            // So do not call super.doDispose();
        }
    }

    protected void doDispose()
    {
        if (socket != null && !socket.isClosed())
        {
            try
            {
                ((MulticastSocket) socket).leaveGroup(inetAddress);
            }
            catch (IOException e)
            {
                logger.error("failed to leave group: " + e.getMessage(), e);
            }
        }
        super.doDispose();
    }

}
