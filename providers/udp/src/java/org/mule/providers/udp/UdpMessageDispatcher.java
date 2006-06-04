/*
 * $Id$
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

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;

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

    public UdpMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (UdpConnector)endpoint.getConnector();
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception {
        if(!connected.get()) {
            URI uri = endpoint.getEndpointURI().getUri();
            port = uri.getPort();
            inetAddress = InetAddress.getByName(uri.getHost());
            socket = createSocket(port, inetAddress);
        }
    }

    protected void doDisconnect() throws Exception {
        try {
            if (socket != null) {
                socket.close();
            }
        } finally {
            socket=null;
        }
    }



    protected DatagramSocket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        DatagramSocket socket = new DatagramSocket();
        socket.setReceiveBufferSize(connector.getBufferSize());
        socket.setSendBufferSize(connector.getBufferSize());
        socket.setSoTimeout(connector.getTimeout());
        return socket;
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
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

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        // If we're doing sync receive try and read return info from socket
        if (event.getEndpoint().isRemoteSync()) {
            DatagramPacket result = receive(socket, event.getTimeout());
            if (result == null) {
                return null;
            }
            UMOMessage message = new MuleMessage(connector.getMessageAdapter(result), event.getMessage());
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

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout  the maximum time the operation should block before returning. The call should
     *                 return immediately if there is data available. If no data becomes available before the timeout
     *                 elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     *         avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception {
        DatagramPacket result = receive(socket, (int)timeout);
        if (result == null) {
            return null;
        }
        UMOMessage message = new MuleMessage(connector.getMessageAdapter(result), (Map)null);
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

    protected void doDispose()
    {
        // template method
    }
}
