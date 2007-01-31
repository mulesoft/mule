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

import org.mule.config.i18n.Message;
import org.mule.providers.AbstractConnector;
import org.mule.providers.tcp.protocols.DefaultProtocol;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.ClassUtils;
import org.mule.util.MapUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>TcpConnector</code> can bind or sent to a given TCP port on a given host.
 */
public class TcpConnector extends AbstractConnector
{
    /**
     * Property can be set on the endpoint to configure how the socket is managed
     */
    public static final String KEEP_SEND_SOCKET_OPEN_PROPERTY = "keepSendSocketOpen";

    public static final int DEFAULT_SOCKET_TIMEOUT = INT_VALUE_NOT_SET;

    public static final int DEFAULT_BUFFER_SIZE = INT_VALUE_NOT_SET;

    public static final int DEFAULT_BACKLOG = INT_VALUE_NOT_SET;

    protected int sendTimeout = DEFAULT_SOCKET_TIMEOUT;

    protected int receiveTimeout = DEFAULT_SOCKET_TIMEOUT;

    protected int bufferSize = DEFAULT_BUFFER_SIZE;

    protected int backlog = DEFAULT_BACKLOG;

    protected String tcpProtocolClassName = DefaultProtocol.class.getName();

    protected TcpProtocol tcpProtocol;

    protected boolean keepSendSocketOpen = false;

    protected boolean keepAlive = false;

    protected Map dispatcherSockets = new HashMap();

    public boolean isKeepSendSocketOpen()
    {
        return keepSendSocketOpen;
    }

    protected void doInitialise() throws InitialisationException
    {

        if (tcpProtocol == null)
        {
            try
            {
                tcpProtocol = (TcpProtocol)ClassUtils.instanciateClass(tcpProtocolClassName, null);
            }
            catch (Exception e)
            {
                throw new InitialisationException(new Message("tcp", 3), e);
            }
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public String getProtocol()
    {
        return "tcp";
    }

    /**
     * A shorthand property setting timeout for both SEND and RECEIVE sockets.
     */
    public void setTimeout(int timeout)
    {
        setSendTimeout(timeout);
        setReceiveTimeout(timeout);
    }

    public int getSendTimeout()
    {
        return this.sendTimeout;
    }

    public void setSendTimeout(int timeout)
    {
        if (timeout < 0)
        {
            timeout = DEFAULT_SOCKET_TIMEOUT;
        }
        this.sendTimeout = timeout;
    }

    // ////////////////////////////////////////////
    // New independednt Socket timeout for receiveSocket
    // ////////////////////////////////////////////
    public int getReceiveTimeout()
    {
        return receiveTimeout;
    }

    public void setReceiveTimeout(int timeout)
    {
        if (timeout < 0)
        {
            timeout = DEFAULT_SOCKET_TIMEOUT;
        }
        this.receiveTimeout = timeout;
    }

    public int getBufferSize()
    {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize)
    {
        if (bufferSize < 1)
        {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }
        this.bufferSize = bufferSize;
    }

    public int getBacklog()
    {
        return backlog;
    }

    public void setBacklog(int backlog)
    {
        this.backlog = backlog;
    }

    public TcpProtocol getTcpProtocol()
    {
        return tcpProtocol;
    }

    public void setTcpProtocol(TcpProtocol tcpProtocol)
    {
        this.tcpProtocol = tcpProtocol;
    }

    public String getTcpProtocolClassName()
    {
        return tcpProtocolClassName;
    }

    public void setTcpProtocolClassName(String protocolClassName)
    {
        this.tcpProtocolClassName = protocolClassName;
    }

    public boolean isRemoteSyncEnabled()
    {
        return true;
    }

    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }

    /**
     * Well get the output stream (if any) for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint. If
     * Streaming is not supported by this transport an
     * {@link UnsupportedOperationException} is thrown
     * 
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     * @throws org.mule.umo.UMOException
     */
    // TODO HH: Is this the right thing to do? not sure how else to get the
    // outputstream
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message)
        throws UMOException
    {
        try
        {
            Socket socket = getSocket(endpoint);
            if (socket == null)
            {
                // This shouldn't happen
                throw new IllegalStateException("could not get socket for endpoint: "
                                + endpoint.getEndpointURI().getAddress());
            }
            return new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }
        catch (IOException e)
        {
            throw new DispatchException(message, endpoint);
        }
        catch (URISyntaxException e)
        {
            throw new DispatchException(message, endpoint);
        }
    }

    /**
     * Lookup a socket in the list of dispatcher sockets but don't create a new
     * socket
     * 
     * @param endpoint
     * @return
     */
    Socket lookupSocket(UMOImmutableEndpoint endpoint)
    {
        Socket socket;
        synchronized (dispatcherSockets)
        {
            socket = (Socket)dispatcherSockets.remove(endpoint.getEndpointURI().getAddress());
        }
        return socket;
    }

    Socket getSocket(UMOImmutableEndpoint endpoint) throws IOException, URISyntaxException
    {
        Socket socket = lookupSocket(endpoint);

        if (socket == null)
        {
            socket = initSocket(endpoint.getEndpointURI().getUri());
        }
        else if (!socket.isConnected() || socket.isClosed())
        {
            logger.debug("The current socket connection for this endpoint is closed. Creating new connection");
            socket = initSocket(endpoint.getEndpointURI().getUri());
        }
        return socket;
    }

    void releaseSocket(Socket socket, UMOImmutableEndpoint endpoint)
    {
        boolean keepSocketOpen = MapUtils.getBooleanValue(endpoint.getProperties(),
            KEEP_SEND_SOCKET_OPEN_PROPERTY, isKeepSendSocketOpen());
        if (!keepSocketOpen)
        {
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
            catch (IOException e)
            {
                logger.debug("Failed to close socket after dispatch", e);
            }
        }
        else if (socket != null && !socket.isClosed())
        {
            synchronized (dispatcherSockets)
            {
                dispatcherSockets.put(endpoint.getEndpointURI().getAddress(), socket);
            }
        }
    }

    protected Socket initSocket(URI endpoint) throws IOException, URISyntaxException
    {
        int port = endpoint.getPort();
        InetAddress inetAddress = InetAddress.getByName(endpoint.getHost());
        Socket socket = createSocket(port, inetAddress);
        socket.setReuseAddress(true);
        if (getBufferSize() != UMOConnector.INT_VALUE_NOT_SET
                        && socket.getReceiveBufferSize() != getBufferSize())
        {
            socket.setReceiveBufferSize(getBufferSize());
        }
        if (getBufferSize() != UMOConnector.INT_VALUE_NOT_SET
                        && socket.getSendBufferSize() != getBufferSize())
        {
            socket.setSendBufferSize(getBufferSize());
        }
        if (getReceiveTimeout() != UMOConnector.INT_VALUE_NOT_SET
                        && socket.getSoTimeout() != getReceiveTimeout())
        {
            socket.setSoTimeout(getReceiveTimeout());
        }
        return socket;
    }

    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        return new Socket(inetAddress, port);
    }
}
