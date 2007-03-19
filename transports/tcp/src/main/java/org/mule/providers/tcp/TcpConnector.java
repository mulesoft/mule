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
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractConnector;
import org.mule.providers.tcp.protocols.DefaultProtocol;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

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

    protected int sendBufferSize = DEFAULT_BUFFER_SIZE;

    protected int receiveBufferSize = DEFAULT_BUFFER_SIZE;

    protected int receiveBacklog = DEFAULT_BACKLOG;

    protected boolean sendTcpNoDelay;

    protected int socketLinger = INT_VALUE_NOT_SET;

    protected String tcpProtocolClassName = DefaultProtocol.class.getName();

    protected TcpProtocol tcpProtocol;

    protected boolean keepSendSocketOpen = false;

    protected boolean keepAlive = false;

    protected GenericKeyedObjectPool dispatcherSocketsPool = new GenericKeyedObjectPool();

    public boolean isKeepSendSocketOpen()
    {
        return keepSendSocketOpen;
    }

    public void setKeepSendSocketOpen(boolean keepSendSocketOpen)
    {
        this.keepSendSocketOpen = keepSendSocketOpen;
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
        dispatcherSocketsPool.setFactory(new TcpSocketFactory());
        dispatcherSocketsPool.setTestOnBorrow(true);
        dispatcherSocketsPool.setTestOnReturn(true);
        //There should only be one pooled instance per socket (key)        
        dispatcherSocketsPool.setMaxActive(1);
    }

    protected void doDispose()
    {
        try
        {
            dispatcherSocketsPool.close();
        }
        catch (Exception e)
        {
            logger.warn("Failed to close dispatcher socket pool: " + e.getMessage());
        }
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        dispatcherSocketsPool.clear();
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
     * @deprecated The time out should be set explicitly for each
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

    /**
     *
     * @return
     * @deprecated Should use {@link #getSendBufferSize()} or {@link #getReceiveBufferSize()}
     */
    public int getBufferSize()
    {
        return sendBufferSize;
    }

    /**
     *
     * @param bufferSize
     * @deprecated Should use {@link #setSendBufferSize(int)} or {@link #setReceiveBufferSize(int)}
     */
    public void setBufferSize(int bufferSize)
    {
        if (bufferSize < 1)
        {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }
        this.sendBufferSize = bufferSize;
    }


    public int getSendBufferSize()
    {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize)
    {
        if (sendBufferSize < 1)
        {
            sendBufferSize = DEFAULT_BUFFER_SIZE;
        }
        this.sendBufferSize = sendBufferSize;
    }

    public int getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize)
    {
        if (receiveBufferSize < 1)
        {
            receiveBufferSize = DEFAULT_BUFFER_SIZE;
        }
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getReceiveBacklog()
    {
        return receiveBacklog;
    }

    public void setReceiveBacklog(int receiveBacklog)
    {
        if(receiveBacklog < 0)
        {
            receiveBacklog = DEFAULT_BACKLOG;
        }
        this.receiveBacklog = receiveBacklog;
    }

    public int getSendSocketLinger()
    {
        return socketLinger;
    }

    public void setSendSocketLinger(int soLinger)
    {
        if(soLinger < 0)
        {
            soLinger = INT_VALUE_NOT_SET;
        }
        this.socketLinger = soLinger;
    }

    /**
     *
     * @return
     * @deprecated should use {@link #getReceiveBacklog()}
     */
    public int getBacklog()
    {
        return receiveBacklog;
    }

    /**
     *
     * @param backlog
     * @deprecated should use {@link #setReceiveBacklog(int)}
     */
    public void setBacklog(int backlog)
    {
        this.receiveBacklog = backlog;
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
    // TODO HH: Is this the right thing to do? not sure how else to get the outputstream
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message)
        throws UMOException
    {

        Socket socket;
        try
        {
            socket = getSocket(endpoint);
        }
        catch (Exception e)
        {
            throw new MessagingException(new Message(Messages.FAILED_TO_GET_OUTPUT_STREAM), message, e);
        }
        if (socket == null)
        {
            // This shouldn't happen
            throw new IllegalStateException("could not get socket for endpoint: "
                                            + endpoint.getEndpointURI().getAddress());
        }
        try
        {
            return new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }
        catch (IOException e)
        {
            throw new MessagingException(new Message(Messages.FAILED_TO_GET_OUTPUT_STREAM), message, e);
        }

    }

    /**
     * Lookup a socket in the list of dispatcher sockets but don't create a new
     * socket
     * 
     * @param endpoint
     * @return
     */
    Socket getSocket(UMOImmutableEndpoint endpoint) throws Exception
    {
        return (Socket)dispatcherSocketsPool.borrowObject(endpoint);
    }

    void releaseSocket(Socket socket, UMOImmutableEndpoint endpoint) throws Exception
    {
        dispatcherSocketsPool.returnObject(endpoint, socket);
    }

    public boolean isSendTcpNoDelay()
    {
        return sendTcpNoDelay;
    }

    public void setSendTcpNoDelay(boolean sendTcpNoDelay)
    {
        this.sendTcpNoDelay = sendTcpNoDelay;
    }
}
