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
package org.mule.providers.udp;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import org.mule.DisposeException;
import org.mule.InitialisationException;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.provider.UMOMessageAdapter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * <code>UdpMessageReceiver</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class UdpMessageReceiver extends AbstractMessageReceiver implements Runnable
{
    protected DatagramSocket socket = null;

    protected PooledExecutor threadPool;

    protected InetAddress inetAddress;

    protected int bufferSize;

    private URI uri;

    private Thread worker;

    public UdpMessageReceiver(AbstractConnector connector,
                              UMOComponent component,
                              UMOEndpoint endpoint) throws InitialisationException
    {
        create(connector, component, endpoint);
        bufferSize = ((UdpConnector) connector).getBufferSize();

        threadPool = connector.getReceiverThreadingProfile().createPool(connector.getName());
        uri = endpoint.getEndpointURI().getUri();

        try
        {
            inetAddress = InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e)
        {
            throw new InitialisationException("Failed to locate host: " + e.getMessage(), e);
        }
        connect(uri);
        worker = new Thread(this);
        worker.start();
    }

    protected void connect(URI uri) throws InitialisationException
    {

        logger.info("Attempting to connect to: " + uri.toString());
        int count = ((UdpConnector) connector).getRetryCount();
        long freq = ((UdpConnector) connector).getRetryFrequency();
        count++;
        for (int i = 0; i < count; i++)
        {
            try
            {
                socket = createSocket(uri, inetAddress);
                socket.setSoTimeout(((UdpConnector) connector).getTimeout());
                socket.setReceiveBufferSize(bufferSize);
                socket.setSendBufferSize(bufferSize);
                logger.info("Connected to: " + uri.toString());
                break;
            } catch (Exception e)
            {
                logger.debug("Failed to bind to uri: " + uri, e);
                if (i < count - 1)
                {
                    try
                    {
                        Thread.sleep(freq);
                    } catch (InterruptedException ignore)
                    {
                    }
                } else
                {
                    throw new InitialisationException("Unable to bind to uri: " + uri + ". Reason: " + e);
                }
            }
        }
    }

    protected DatagramSocket createSocket(URI uri, InetAddress inetAddress) throws IOException
    {
        return new DatagramSocket(uri.getPort(), inetAddress);
    }

    /**
     * Obtain the serverSocket
     */
    public DatagramSocket getSocket()
    {
        return socket;
    }

    protected DatagramPacket createPacket()
    {
        DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);
        if(uri.getPort() > 0) {
            packet.setPort(uri.getPort());
        }
        packet.setAddress(inetAddress);
        return packet;
    }
    public void run()
    {
        while(!disposing.get()) {
            if(connector.isStarted()) {

                try
                {
                    DatagramPacket packet = createPacket();
                    try
                    {
                        socket.receive(packet);
                        logger.trace("Received packet on: " + inetAddress.toString());
                        Runnable worker = createWorker(packet);
                        try
                        {
                            threadPool.execute(worker);
                        } catch (InterruptedException e)
                        {
                            logger.error("Udp receiver interrupted: " + e.getMessage(), e);
                        }
                    } catch (SocketTimeoutException e)
                    {
                        //ignore
                    }

                } catch (Exception e)
                {
                    if(!connector.isDisposed() && ! disposing.get()) {
                        logger.debug("Accept failed on socket: " + e, e);
                        handleException(null, e);
                    }
                }
            }
        }
    }

    public void doDispose() throws UMOException
    {
        try
        {
            threadPool.shutdownNow();
            socket.close();

        } catch (Exception e)
        {
            throw new DisposeException("Failed to close udp socket: " + e.getMessage(), e);
        }
        logger.info("Closed Udp connection: " + uri);
    }

    protected Runnable createWorker(DatagramPacket packet) throws IOException
    {
        return new UdpWorker(new DatagramSocket(0), packet);
    }


    protected class UdpWorker implements Runnable, Disposable
    {
        private DatagramSocket socket = null;
        private DatagramPacket packet;

        public UdpWorker(DatagramSocket socket, DatagramPacket packet)
        {
            this.socket = socket;
            this.packet = packet;
        }

        public void dispose()
        {
            if (socket != null) socket.close();
            socket = null;
        }

        /**
         * Accept requests from a given Udp address
         */
        public void run()
        {
            try
            {
                UMOMessageAdapter adapter = connector.getMessageAdapter(packet);
                UMOMessage returnMessage = routeMessage(new MuleMessage(adapter),  connector.isSynchronous());

                if (returnMessage != null)
                {
                    byte[] data = returnMessage.getPayloadAsBytes();
                    DatagramPacket result = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    socket.send(result);
                }
            } catch (Exception e)
            {
                handleException("Failed to process Udp Request on: "
                        + (socket != null ? socket.getInetAddress().toString() : "null"),
                        e);
            } finally
            {
                try
                {
                    socket.close();
                } catch (Exception e)
                {
                    logger.error("Socket close failed with: " + e);
                }
            }
        }
    }
}