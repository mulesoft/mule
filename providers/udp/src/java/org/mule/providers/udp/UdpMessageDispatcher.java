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
package org.mule.providers.udp;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

/**
 * <code>UdpMessageDispatcher</code> is responsible for dispatching MuleEvents as
 * UDP packets on the network
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UdpMessageDispatcher extends AbstractMessageDispatcher
{
    protected UdpConnector connector;
    protected InetAddress inetAddress;
    protected DatagramSocket socket;
    protected int port;
    protected AtomicBoolean initialised = new AtomicBoolean(false);

    public UdpMessageDispatcher(UdpConnector connector)
    {
        super(connector);
        this.connector = connector;
    }

    protected DatagramSocket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        return new DatagramSocket(port, inetAddress);
    }

    protected void initialise(String endpoint) throws IOException, URISyntaxException
    {
        if (!initialised.get()) {
            URI uri = new URI(endpoint);
            port = uri.getPort();
            inetAddress = InetAddress.getByName(uri.getHost());
            socket = createSocket(port, inetAddress);
            socket.setReceiveBufferSize(connector.getBufferSize());
            socket.setSendBufferSize(connector.getBufferSize());
            socket.setSoTimeout(connector.getTimeout());
            initialised.set(true);
        }
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        initialise(event.getEndpoint().getEndpointURI().getAddress());
        byte[] payload = event.getTransformedMessageAsBytes();
        write(socket, payload);
    }

    protected void write(DatagramSocket socket, byte[] data) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(data, data.length);
        if (port >= 0) {
            packet.setPort(port);
       }
        packet.setAddress(inetAddress);
        socket.send(packet);
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        // If we're doing sync receive try and read return info from socket
        if (event.getEndpoint().isRemoteSync()) {
            DatagramPacket result = receive(socket, event.getEndpoint().getRemoteSyncTimeout());
            if (result == null)
                return null;
            UMOMessage message = new MuleMessage(connector.getMessageAdapter(result));
            return message;
        } else {
            return event.getMessage();
        }
    }

    private DatagramPacket receive(DatagramSocket socket, int timeout) throws IOException
    {
        int origTimeout = socket.getSoTimeout();
        try {
            DatagramPacket packet = new DatagramPacket(new byte[connector.getBufferSize()], connector.getBufferSize());
            socket.setSoTimeout(timeout);
            socket.receive(packet);
            return packet;
        } finally {
            socket.setSoTimeout(origTimeout);
        }
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        initialise(endpointUri.getAddress());
        DatagramPacket result = receive(socket, Integer.parseInt(String.valueOf(timeout)));
        if (result == null)
            return null;
        UMOMessage message = new MuleMessage(connector.getMessageAdapter(result));
        return message;
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose()
    {
        initialised.set(false);
        if (socket != null)
            socket.close();
    }
}
