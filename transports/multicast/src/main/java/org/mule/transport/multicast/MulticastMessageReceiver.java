/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.multicast;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.transport.AbstractConnector;
import org.mule.transport.udp.UdpMessageReceiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;

import javax.resource.spi.work.Work;

public class MulticastMessageReceiver extends UdpMessageReceiver
{

    public MulticastMessageReceiver(AbstractConnector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
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

    @Override
    public boolean shouldConsumeInEveryNode()
    {
        return false;
    }
}
