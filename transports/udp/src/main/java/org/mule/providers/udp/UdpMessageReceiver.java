/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.transformer.UMOTransformer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

/**
 * <code>UdpMessageReceiver</code> receives UDP message packets.
 */
public class UdpMessageReceiver extends AbstractMessageReceiver implements Work
{
    protected DatagramSocket socket = null;
    protected InetAddress inetAddress;
    protected int bufferSize;
    private URI uri;
    protected UMOTransformer responseTransformer = null;

    public UdpMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
        throws InitialisationException
    {
        super(connector, component, endpoint);
        bufferSize = ((UdpConnector)connector).getReceiveBufferSize();

        uri = endpoint.getEndpointURI().getUri();

        try
        {
            if(!"null".equalsIgnoreCase(uri.getHost()))
            {
                inetAddress = InetAddress.getByName(uri.getHost());
            }
        }
        catch (UnknownHostException e)
        {
            throw new InitialisationException(new Message("udp", 2, uri), e, this);
        }

        responseTransformer = getResponseTransformer();
    }

    protected void doConnect() throws Exception
    {
        try
        {
            socket = ((UdpConnector)connector).getSocket(endpoint);
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message("udp", 1, uri), e, this);
        }

        try
        {
            getWorkManager().scheduleWork(this, WorkManager.INDEFINITE, null, connector);
        }
        catch (WorkException e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_TO_SCHEDULE_WORK), e, this);
        }
    }

    protected void doDisconnect() throws Exception
    {
        // this will cause the server thread to quit
        disposing.set(true);
        if (socket != null)
        {
            socket.close();
        }

    }

    protected void doStart() throws UMOException
    {
        // nothing to do
    }

    protected void doStop() throws UMOException
    {
        // nothing to do
    }

    protected UMOTransformer getResponseTransformer() throws InitialisationException
    {
        UMOTransformer transformer = endpoint.getResponseTransformer();
        if (transformer == null)
        {
            return connector.getDefaultResponseTransformer();
        }
        return transformer;
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
//        if (uri.getPort() > 0)
//        {
//            packet.setPort(uri.getPort());
//        }
//        packet.setAddress(inetAddress);
        return packet;
    }

    public void run()
    {
        while (!disposing.get())
        {
            if (connector.isStarted())
            {

                try
                {
                    DatagramPacket packet = createPacket();
                    try
                    {
                        socket.receive(packet);

                        if (logger.isTraceEnabled())
                        {
                            logger.trace("Received packet on: " + uri);
                        }

                        Work work = createWork(packet);
                        try
                        {
                            getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null, connector);
                        }
                        catch (WorkException e)
                        {
                            logger.error("Udp receiver interrupted: " + e.getMessage(), e);
                        }
                    }
                    catch (SocketTimeoutException e)
                    {
                        // ignore
                    }

                }
                catch (Exception e)
                {
                    if (!connector.isDisposed() && !disposing.get())
                    {
                        logger.debug("Accept failed on socket: " + e, e);
                        handleException(e);
                    }
                }
            }
        }
    }

    public void release()
    {
        dispose();
    }

    protected void doDispose()
    {
        if (socket != null && !socket.isClosed())
        {
            logger.debug("Closing Udp connection: " + uri);
            socket.close();
            logger.info("Closed Udp connection: " + uri);
        }
    }

    protected Work createWork(DatagramPacket packet) throws IOException
    {
        return new UdpWorker(new DatagramSocket(0), packet);
    }

    protected class UdpWorker implements Work, Disposable
    {
        private DatagramSocket socket = null;
        private DatagramPacket packet;

        public UdpWorker(DatagramSocket socket, DatagramPacket packet)
        {
            this.socket = socket;
            this.packet = packet;
        }

        public void release()
        {
            dispose();
        }

        public void dispose()
        {
            if (socket != null && !socket.isClosed())
            {
                try
                {
                    socket.close();
                }
                catch (Exception e)
                {
                    logger.error("Socket close failed", e);
                }
            }
            socket = null;
        }

        /**
         * Accept requests from a given Udp address
         */
        public void run()
        {
            UMOMessage returnMessage = null;
            try
            {
                UMOMessageAdapter adapter = connector.getMessageAdapter(packet);
                returnMessage = routeMessage(new MuleMessage(adapter), endpoint.isSynchronous());

                if (returnMessage != null)
                {
                    byte[] data;
                    if (responseTransformer != null)
                    {
                        Object response = responseTransformer.transform(returnMessage.getPayload());
                        if (response instanceof byte[])
                        {
                            data = (byte[])response;
                        }
                        else
                        {
                            data = response.toString().getBytes();
                        }
                    }
                    else
                    {
                        data = returnMessage.getPayloadAsBytes();
                    }
                    DatagramPacket result = new DatagramPacket(data, data.length, packet.getAddress(),
                        packet.getPort());
                    socket.send(result);
                }
            }
            catch (Exception e)
            {
                if (!disposing.get())
                {
                    handleException(e);
                }
            }
            finally
            {
                dispose();
            }
        }
    }
}
