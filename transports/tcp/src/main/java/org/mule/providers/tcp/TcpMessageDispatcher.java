/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.MapUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * <code>TcpMessageDispatcher</code> will send transformed Mule events over TCP.
 */

public class TcpMessageDispatcher extends AbstractMessageDispatcher
{
    private final TcpConnector connector;
    protected final SerializableToByteArray serializableToByteArray;
    protected Socket connectedSocket = null;
    protected boolean keepSendSocketOpen = false;

    public TcpMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (TcpConnector)endpoint.getConnector();
        serializableToByteArray = new SerializableToByteArray();
    }

    protected Socket initSocket(String endpoint) throws IOException, URISyntaxException
    {
        URI uri = new URI(endpoint);
        int port = uri.getPort();
        InetAddress inetAddress = InetAddress.getByName(uri.getHost());
        Socket socket = createSocket(port, inetAddress);
        socket.setReuseAddress(true);
        if (connector.getBufferSize() != UMOConnector.INT_VALUE_NOT_SET
            && socket.getReceiveBufferSize() != connector.getBufferSize())
        {
            socket.setReceiveBufferSize(connector.getBufferSize());
        }
        if (connector.getBufferSize() != UMOConnector.INT_VALUE_NOT_SET
            && socket.getSendBufferSize() != connector.getBufferSize())
        {
            socket.setSendBufferSize(connector.getBufferSize());
        }
        if (connector.getReceiveTimeout() != UMOConnector.INT_VALUE_NOT_SET
            && socket.getSoTimeout() != connector.getReceiveTimeout())
        {
            socket.setSoTimeout(connector.getReceiveTimeout());
        }
        return socket;
    }

    protected synchronized void doDispatch(UMOEvent event) throws Exception
    {
        try
        {
            doInternalDispatch(event);
        }
        finally
        {
            if (!keepSendSocketOpen)
            {
                doDispose();
            }
        }
    }

    protected synchronized UMOMessage doSend(UMOEvent event) throws Exception
    {
        doInternalDispatch(event);

        if (useRemoteSync(event))
        {
            try
            {
                Object result = receive(connectedSocket, event.getTimeout());
                if (result == null)
                {
                    return null;
                }
                return new MuleMessage(connector.getMessageAdapter(result));
            }
            catch (SocketTimeoutException e)
            {
                // we don't necessarily expect to receive a response here
                logger.info("Socket timed out normally while doing a synchronous receive on endpointUri: "
                            + event.getEndpoint().getEndpointURI());
                return null;
            }
        }
        else
        {
            return event.getMessage();
        }
    }

    /**
     * The doSend() and doDispatch() methods need to handle socket disposure
     * differently, thus the need to extract this common code.
     * 
     * @param event event
     * @throws Exception in case of any error
     */
    protected void doInternalDispatch(UMOEvent event) throws Exception
    {
        Object payload = event.getTransformedMessage();

        // utilize the value after any endpoint overrides, check for dead socket
        if (!keepSendSocketOpen || connectedSocket == null || connectedSocket.isClosed())
        {
            connectedSocket = initSocket(endpoint.getEndpointURI().getAddress());
        }

        try
        {
            write(connectedSocket, payload);
            // If we're doing sync receive try and read return info from socket
        }
        catch (IOException e)
        {
            if (keepSendSocketOpen)
            {
                logger.warn("Write raised exception: '" + e.getMessage() + "' attempting to reconnect.");
                // Try reconnecting or a Fatal Connection Exception will be thrown
                reconnect();
                write(connectedSocket, payload);
            }
            else
            {
                throw e;
            }
        }
    }

    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        return new Socket(inetAddress, port);
    }

    protected void write(Socket socket, Object data) throws IOException, TransformerException
    {
        TcpProtocol protocol = connector.getTcpProtocol();

        byte[] binaryData;
        if (data instanceof String)
        {
            binaryData = data.toString().getBytes();
        }
        else if (data instanceof byte[])
        {
            binaryData = (byte[])data;
        }
        else
        {
            binaryData = (byte[])serializableToByteArray.transform(data);
        }

        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        protocol.write(bos, binaryData);
        bos.flush();
    }

    protected Object receive(Socket socket, int timeout) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        if (timeout >= 0)
        {
            socket.setSoTimeout(timeout);
        }
        // TODO SF: check if this cast is ok!
        return (byte[])connector.getTcpProtocol().read(dis);
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        Socket socket = null;
        try
        {
            socket = initSocket(endpoint.getEndpointURI().getAddress());
            try
            {
                Object result = receive(socket, (int)timeout);
                if (result == null)
                {
                    return null;
                }
                return new MuleMessage(connector.getMessageAdapter(result));
            }
            catch (SocketTimeoutException e)
            {
                // we don't necesarily expect to receive a resonse here
                logger.info("Socket timed out normally while doing a synchronous receive on endpointUri: "
                            + endpoint.getEndpointURI());
                return null;
            }
        }
        finally
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();
            }
        }
    }

    /**
     * Well get the output stream (if any) for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint
     * 
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     * @throws org.mule.umo.UMOException
     */
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message)
        throws UMOException
    {
        try
        {
            return connectedSocket.getOutputStream();
        }
        catch (IOException e)
        {
            throw new DispatchException(message, endpoint, e);
        }
    }

    protected synchronized void doDispose()
    {
        try
        {
            doDisconnect();
        }
        catch (Exception e)
        {
            logger.error("Failed to shutdown the dispatcher.", e);
        }
    }

    protected void doConnect() throws Exception
    {
        keepSendSocketOpen = MapUtils.getBooleanValue(endpoint.getProperties(), "keepSendSocketOpen",
            connector.isKeepSendSocketOpen());

        if (connectedSocket == null || connectedSocket.isClosed() || !keepSendSocketOpen)
        {
            connectedSocket = initSocket(endpoint.getEndpointURI().getAddress());
        }
    }

    protected void doDisconnect() throws Exception
    {
        if (null != connectedSocket && !connectedSocket.isClosed())
        {
            try
            {
                connectedSocket.close();
                connectedSocket = null;
            }
            catch (IOException e)
            {
                logger.warn("ConnectedSocked.close() raised exception. Reason: " + e.getMessage());
            }
        }
    }
}
