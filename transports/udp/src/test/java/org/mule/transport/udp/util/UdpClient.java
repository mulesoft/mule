/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp.util;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpClient
{
    private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 512;

    private int port;
    private InetAddress host;
    private int soTimeout = AbstractMuleContextTestCase.RECEIVE_TIMEOUT;
    private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;
    private DatagramSocket socket;

    public UdpClient(int port) throws UnknownHostException
    {
        super();
        this.port = port;
        this.host = InetAddress.getByName("localhost");
    }

    public byte[] send(String string) throws IOException
    {
        return send(string.getBytes());
    }

    public byte[] send(byte[] bytes) throws IOException
    {
        dispatch(bytes);

        byte[] receiveBuffer = new byte[receiveBufferSize];
        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(packet);

        return packet.getData();
    }

    public void dispatch(byte[] bytes) throws IOException
    {
        initSocket();

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, host, port);
        socket.send(packet);
    }

    private void initSocket() throws SocketException
    {
        if (socket == null)
        {
            socket = new DatagramSocket();
            socket.setSoTimeout(soTimeout);
        }
    }

    public void shutdown()
    {
        if (socket != null)
        {
            socket.close();
        }
    }
}
