/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.config.i18n.CoreMessages;
import org.mule.model.streaming.CallbackOutputStream;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConfigurableKeyedObjectPool;
import org.mule.transport.tcp.protocols.SafeProtocol;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.util.monitor.ExpiryMonitor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * <code>TcpConnector</code> can bind or sent to a given TCP port on a given host.
 * Other socket-based transports can be built on top of this class by providing the
 * appropriate socket factories and application level protocols as required (see
 * the constructor and the SSL transport for examples).
 */
public class TcpConnector extends AbstractConnector
{
    public static final String TCP = "tcp";
    public static final String SEND_TCP_NO_DELAY_SYSTEM_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX
                                                                   + "transport.tcp.defaultSendTcpNoDelay";

    /** Property can be set on the endpoint to configure how the socket is managed */
    public static final String KEEP_SEND_SOCKET_OPEN_PROPERTY = "keepSendSocketOpen";
    public static final int DEFAULT_SOCKET_TIMEOUT = INT_VALUE_NOT_SET;
    public static final int DEFAULT_SO_LINGER = INT_VALUE_NOT_SET;
    public static final int DEFAULT_BUFFER_SIZE = INT_VALUE_NOT_SET;
    public static final int DEFAULT_BACKLOG = INT_VALUE_NOT_SET;
    public static final int DEFAULT_WAIT_TIMEOUT = INT_VALUE_NOT_SET;

    // to clarify arg to configureSocket
    public static final boolean SERVER = false;
    public static final boolean CLIENT = true;

    private int clientSoTimeout = DEFAULT_SOCKET_TIMEOUT;
    private int serverSoTimeout = DEFAULT_SOCKET_TIMEOUT;
    private int connectionTimeout = DEFAULT_SOCKET_TIMEOUT;
    private int socketMaxWait = DEFAULT_WAIT_TIMEOUT;
    private int sendBufferSize = DEFAULT_BUFFER_SIZE;
    private int receiveBufferSize = DEFAULT_BUFFER_SIZE;
    private int receiveBacklog = DEFAULT_BACKLOG;
    private boolean sendTcpNoDelay;
    private Boolean reuseAddress = Boolean.TRUE; // this could be null for Java default
    private int socketSoLinger = DEFAULT_SO_LINGER;
    private TcpProtocol tcpProtocol;
    private AbstractTcpSocketFactory socketFactory;
    private SimpleServerSocketFactory serverSocketFactory;
    private GenericKeyedObjectPool socketsPool = new GenericKeyedObjectPool();
    private int keepAliveTimeout = 0;
    private ExpiryMonitor keepAliveMonitor;
    private Boolean failOnUnresolvedHost = Boolean.TRUE;

    /** 
     * If set, the socket is not closed after sending a message.  This attribute 
     * only applies when sending data over a socket (Client).
     */
    private boolean keepSendSocketOpen = false;

    /**
     * Enables SO_KEEPALIVE behavior on open sockets. This automatically checks 
     * socket connections that are open but unused for long periods and closes 
     * them if the connection becomes unavailable.  This is a property on the 
     * socket itself and is used by a server socket to control whether 
     * connections to the server are kept alive before they are recycled.
     */
    private boolean keepAlive = false;

    //TODO MULE-2300 remove once fixed
    private TcpSocketKey lastSocketKey;

    public TcpConnector(MuleContext context)
    {
        super(context);
        // Default tcpNoDelay=false if system property not set.
        sendTcpNoDelay = Boolean.valueOf(System.getProperty(SEND_TCP_NO_DELAY_SYSTEM_PROPERTY));
        setSocketFactory(new TcpSocketFactory());
        setServerSocketFactory(new TcpServerSocketFactory());
        setTcpProtocol(new SafeProtocol());
    }

    public void configureSocket(boolean client, Socket socket) throws SocketException
    {
        // There is some overhead in setting socket timeout and buffer size, so we're
        // careful here only to set if needed

        if (newValue(getReceiveBufferSize(), socket.getReceiveBufferSize()))
        {
            socket.setReceiveBufferSize(getReceiveBufferSize());
        }
        if (newValue(getSendBufferSize(), socket.getSendBufferSize()))
        {
            socket.setSendBufferSize(getSendBufferSize());
        }
        if (client)
        {
            if (newValue(getClientSoTimeout(), socket.getSoTimeout()))
            {
                socket.setSoTimeout(getClientSoTimeout());
            }
        }
        else
        {
            if (newValue(getServerSoTimeout(), socket.getSoTimeout()))
            {
                socket.setSoTimeout(getServerSoTimeout());
            }
        }
        if (newValue(getSocketSoLinger(), socket.getSoLinger()))
        {
            socket.setSoLinger(true, getSocketSoLinger());
        }
        try
        {
            socket.setTcpNoDelay(isSendTcpNoDelay());
        }
        catch (SocketException e)
        {
            // MULE-2800 - Bug in Solaris
        }
        socket.setKeepAlive(isKeepAlive());
    }

    private boolean newValue(int parameter, int socketValue)
    {
        return parameter != Connector.INT_VALUE_NOT_SET && parameter != socketValue;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        socketFactory.setConnectionTimeout(getConnectionTimeout());

        socketsPool.setFactory(getSocketFactory());
        socketsPool.setTestOnBorrow(true);
        socketsPool.setTestOnReturn(true);
        socketsPool.setMaxActive(getDispatcherThreadingProfile().getMaxThreadsActive());
        socketsPool.setMaxIdle(getDispatcherThreadingProfile().getMaxThreadsIdle());
        socketsPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK);
        socketsPool.setMaxWait(socketMaxWait);

        // Use connector's classloader so that other temporary classloaders
        // aren't used when things are started lazily or from elsewhere.
        final String monitorName = String.format("%s%s.socket",
                                                 ThreadNameHelper.getPrefix(muleContext),
                                                 getName());
        keepAliveMonitor = new ExpiryMonitor(monitorName, 1000, this.getClass().getClassLoader(), muleContext, false);
    }

    @Override
    protected void doDispose()
    {
        logger.debug("Closing TCP connector");
        try
        {
            socketsPool.close();
        }
        catch (Exception e)
        {
            logger.warn("Failed to close dispatcher socket pool: " + e.getMessage());
        }
        
        keepAliveMonitor.dispose();
    }

    /**
     * Lookup a socket in the list of dispatcher sockets but don't create a new
     * socket
     */
    protected Socket getSocket(ImmutableEndpoint endpoint) throws Exception
    {
        TcpSocketKey socketKey = new TcpSocketKey(endpoint);
        if (logger.isDebugEnabled())
        {
            logger.debug("borrowing socket for " + socketKey + "/" + socketKey.hashCode());
            if (null != lastSocketKey)
            {
                logger.debug("same as " + lastSocketKey.hashCode() + "? " + lastSocketKey.equals(socketKey));
            }
        }
        Socket socket = (Socket) socketsPool.borrowObject(socketKey);
        if (logger.isDebugEnabled())
        {
            logger.debug("borrowed socket, "
                    + (socket.isClosed() ? "closed" : "open") 
                    + "; debt " + socketsPool.getNumActive());
        }
        return socket;
    }

    void releaseSocket(Socket socket, ImmutableEndpoint endpoint) throws Exception
    {
        TcpSocketKey socketKey = new TcpSocketKey(endpoint);
        lastSocketKey = socketKey;
        socketsPool.returnObject(socketKey, socket);
        if (logger.isDebugEnabled())
        {
            logger.debug("returning socket for " + socketKey.hashCode());
            logger.debug("returned socket; debt " + socketsPool.getNumActive());
        }
    }

    public OutputStream getOutputStream(final ImmutableEndpoint endpoint, MuleMessage message)
            throws MuleException
    {
        final Socket socket;
        try
        {
            socket = getSocket(endpoint);
        }
        catch (Exception e)
        {
            throw new MessagingException(CoreMessages.failedToGetOutputStream(), message, e);
        }
        if (socket == null)
        {
            // This shouldn't happen
            throw new IllegalStateException("could not get socket for endpoint: "
                    + endpoint.getEndpointURI().getAddress());
        }
        try
        {
            return new CallbackOutputStream(
                    new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())),
                    new CallbackOutputStream.Callback()
                    {
                        public void onClose() throws Exception
                        {
                            releaseSocket(socket, endpoint);
                        }
                    });
        }
        catch (IOException e)
        {
            throw new MessagingException(CoreMessages.failedToGetOutputStream(), message, e);
        }
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        socketsPool.clear();
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        // template method
    }

    public String getProtocol()
    {
        return TCP;
    }

    // getters and setters ---------------------------------------------------------

    public boolean isKeepSendSocketOpen()
    {
        return keepSendSocketOpen;
    }

    public void setKeepSendSocketOpen(boolean keepSendSocketOpen)
    {
        this.keepSendSocketOpen = keepSendSocketOpen;
    }

    /**
     * A shorthand property setting timeout for both SEND and RECEIVE sockets.
     *
     * @deprecated The time out should be set explicitly for each
     */
    @Deprecated
    public void setTimeout(int timeout)
    {
        setClientSoTimeout(timeout);
        setServerSoTimeout(timeout);
    }

    public int getClientSoTimeout()
    {
        return this.clientSoTimeout;
    }

    public void setClientSoTimeout(int timeout)
    {
        this.clientSoTimeout = valueOrDefault(timeout, 0, DEFAULT_SOCKET_TIMEOUT);
    }

    public int getConnectionTimeout()
    {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(int timeout)
    {
        this.connectionTimeout= valueOrDefault(timeout, 0, DEFAULT_SOCKET_TIMEOUT);
    }

    public int getServerSoTimeout()
    {
        return serverSoTimeout;
    }

    public void setServerSoTimeout(int timeout)
    {
        this.serverSoTimeout = valueOrDefault(timeout, 0, DEFAULT_SOCKET_TIMEOUT);
    }

    public int getSocketMaxWait()
    {
        return socketMaxWait;
    }

    public void setSocketMaxWait(int timeout)
    {
        this.socketMaxWait = valueOrDefault(timeout, 0, DEFAULT_WAIT_TIMEOUT);
    }

    /** @deprecated Should use {@link #getSendBufferSize()} or {@link #getReceiveBufferSize()} */
    @Deprecated
    public int getBufferSize()
    {
        return sendBufferSize;
    }

    /** @deprecated Should use {@link #setSendBufferSize(int)} or {@link #setReceiveBufferSize(int)} */
    @Deprecated
    public void setBufferSize(int bufferSize)
    {
        sendBufferSize = valueOrDefault(bufferSize, 1, DEFAULT_BUFFER_SIZE);
    }

    public int getSendBufferSize()
    {
        return sendBufferSize;
    }

    public void setSendBufferSize(int bufferSize)
    {
        sendBufferSize = valueOrDefault(bufferSize, 1, DEFAULT_BUFFER_SIZE);
    }

    public int getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int bufferSize)
    {
        receiveBufferSize = valueOrDefault(bufferSize, 1, DEFAULT_BUFFER_SIZE);
    }

    public int getReceiveBacklog()
    {
        return receiveBacklog;
    }

    public void setReceiveBacklog(int receiveBacklog)
    {
        this.receiveBacklog = valueOrDefault(receiveBacklog, 0, DEFAULT_BACKLOG);
    }

    public int getSocketSoLinger()
    {
        return socketSoLinger;
    }

    public void setSocketSoLinger(int soLinger)
    {
        this.socketSoLinger = valueOrDefault(soLinger, 0, INT_VALUE_NOT_SET);
    }

    /**
     * @deprecated should use {@link #getReceiveBacklog()}
     */
    @Deprecated
    public int getBacklog()
    {
        return receiveBacklog;
    }

    /**
     * @param backlog
     * @deprecated should use {@link #setReceiveBacklog(int)}
     */
    @Deprecated
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

    @Override
    public boolean isResponseEnabled()
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

    public boolean isSendTcpNoDelay()
    {
        return sendTcpNoDelay;
    }

    public void setSendTcpNoDelay(boolean sendTcpNoDelay)
    {
        this.sendTcpNoDelay = sendTcpNoDelay;
    }

    protected void setSocketFactory(AbstractTcpSocketFactory socketFactory)
    {
        this.socketFactory = socketFactory;
    }

    protected AbstractTcpSocketFactory getSocketFactory()
    {
        return socketFactory;
    }

    public SimpleServerSocketFactory getServerSocketFactory()
    {
        return serverSocketFactory;
    }

    public void setServerSocketFactory(SimpleServerSocketFactory serverSocketFactory)
    {
        this.serverSocketFactory = serverSocketFactory;
    }

    protected ServerSocket getServerSocket(URI uri) throws IOException
    {
        return getServerSocketFactory().createServerSocket(uri, getReceiveBacklog(), isReuseAddress());
    }

    private static int valueOrDefault(int value, int threshhold, int deflt)
    {
        if (value < threshhold)
        {
            return deflt;
        }
        else
        {
            return value;
        }
    }

    /**
     * @return true if the server socket sets SO_REUSEADDRESS before opening
     */
    public Boolean isReuseAddress()
    {
        return reuseAddress;
    }

    /**
     * This allows closed sockets to be reused while they are still in TIME_WAIT state
     *
     * @param reuseAddress Whether the server socket sets SO_REUSEADDRESS before opening
     */
    public void setReuseAddress(Boolean reuseAddress)
    {
        this.reuseAddress = reuseAddress;
    }

    public ExpiryMonitor getKeepAliveMonitor()
    {
        return keepAliveMonitor;
    }
    
    /**
     * @return keep alive timeout in Milliseconds
     */
    public int getKeepAliveTimeout()
    {
        return keepAliveTimeout;
    }
    
    /**
     * Sets the keep alive timeout (in Milliseconds)
     */
    public void setKeepAliveTimeout(int keepAliveTimeout)
    {
        this.keepAliveTimeout = keepAliveTimeout;
    }
    
    @Override
    public void setDispatcherFactory(MessageDispatcherFactory dispatcherFactory)
    {
        if (this.dispatcherFactory == null) {
            super.setDispatcherFactory(dispatcherFactory);
        }
    }

    public ConfigurableKeyedObjectPool getDispatchers()
    {
        return dispatchers;
    }

    public int getSocketsPoolMaxActive()
    {
        return socketsPool.getMaxActive();
    }

    public int getSocketsPoolMaxIdle()
    {
        return socketsPool.getMaxIdle();
    }

    public int getSocketsPoolNumActive()
    {
        return socketsPool.getNumActive();
    }

    public long getSocketsPoolMaxWait()
    {
        return socketsPool.getMaxWait();
    }

    public Boolean isFailOnUnresolvedHost() 
    {
        return failOnUnresolvedHost;
    }

    public void setFailOnUnresolvedHost(Boolean failOnUnresolvedHost) 
    {
        this.failOnUnresolvedHost = failOnUnresolvedHost;
    }

}
