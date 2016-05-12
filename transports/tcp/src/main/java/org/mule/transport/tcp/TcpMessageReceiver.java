/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.DisposeException;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.AbstractReceiverResourceWorker;
import org.mule.transport.ConnectException;
import org.mule.transport.tcp.i18n.TcpMessages;
import org.mule.util.monitor.Expirable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

/**
 * <code>TcpMessageReceiver</code> acts like a TCP server to receive socket
 * requests.
 */
public class TcpMessageReceiver extends AbstractMessageReceiver implements Work
{
    private ServerSocket serverSocket = null;

    protected final AtomicBoolean disposing = new AtomicBoolean(false);
    
    public TcpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void doConnect() throws ConnectException
    {
        disposing.set(false);

        URI uri = endpoint.getEndpointURI().getUri();

        try
        {
            serverSocket = ((TcpConnector) connector).getServerSocket(uri);
        }
        catch (Exception e)
        {
            throw new ConnectException(TcpMessages.failedToBindToUri(uri), e, this);
        }

        try
        {
            getWorkManager().scheduleWork(this, WorkManager.INDEFINITE, null, connector);
        }
        catch (WorkException e)
        {
            throw new ConnectException(CoreMessages.failedToScheduleWork(), e, this);
        }
    }

    @Override
    protected void doDisconnect() throws ConnectException
    {
        // this will cause the server thread to quit
        disposing.set(true);

        try
        {
            if (serverSocket != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Closing: " + serverSocket);
                }
                serverSocket.close();
            }
        }
        catch (IOException e)
        {
            logger.warn("Failed to close server socket: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doStart() throws MuleException
    {
        // nothing to do
    }

    @Override
    protected void doStop() throws MuleException
    {
        // nothing to do
    }

    /**
     * Obtain the serverSocket
     * @return the server socket for this server
     */
    public ServerSocket getServerSocket()
    {
        return serverSocket;
    }

    public void run()
    {
        while (!disposing.get())
        {
            if (connector.isStarted() && !disposing.get())
            {
                try
                {
                    retryTemplate.execute(new RetryCallback()
                    {
                        public void doWork(RetryContext context) throws Exception
                        {
                            Socket socket = null;
                            try
                            {
                                socket = serverSocket.accept();
                            }
                            catch (Exception e)
                            {
                                if (!connector.isDisposed() && !disposing.get())
                                {
                                    throw new ConnectException(e, null);
                                }
                            }

                            if (socket != null)
                            {
                                Work work = createWork(socket);
                                getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null, connector);
                            }
                        }

                        public String getWorkDescription()
                        {
                            return getConnectionDescription();
                        }

                        @Override
                        public Connector getWorkOwner()
                        {
                            return getEndpoint().getConnector();
                        }
                    }, getWorkManager());
                }
                catch (Exception e)
                {
                    getEndpoint().getMuleContext().getExceptionListener().handleException(e);
                }
            }
        }
    }

    public void release()
    {
        // template method
    }

    @Override
    protected void doDispose()
    {
        try
        {
            if (serverSocket != null && !serverSocket.isClosed())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Closing: " + serverSocket);
                }
                serverSocket.close();
            }
            serverSocket = null;
        }
        catch (Exception e)
        {
            logger.error(new DisposeException(TcpMessages.failedToCloseSocket(), e, this));
        }
        logger.info("Closed Tcp port");
    }

    protected Work createWork(Socket socket) throws IOException
    {
        return new TcpWorker(socket, this);
    }

    protected class TcpWorker extends AbstractReceiverResourceWorker implements Disposable, Expirable
    {
        protected Socket socket = null;
        protected TcpInputStream dataIn;
        protected InputStream underlyingIn;
        protected OutputStream dataOut;
        protected TcpProtocol protocol;
        protected boolean dataInWorkFinished = false;
        protected Object notify = new Object();
        private boolean moreMessages = true;
        
        public TcpWorker(Socket socket, AbstractMessageReceiver receiver) throws IOException
        {
            super(socket, receiver, ((TcpConnector) connector).getTcpProtocol().createResponse(socket));
            this.socket = socket;

            final TcpConnector tcpConnector = ((TcpConnector) connector);
            protocol = tcpConnector.getTcpProtocol();

            tcpConnector.configureSocket(TcpConnector.SERVER, socket);

            underlyingIn = new BufferedInputStream(socket.getInputStream());
            dataIn = new TcpInputStream(underlyingIn)
            {
                @Override
                public void close() throws IOException
                {
                    // Don't actually close the stream, we just want to know if the
                    // we want to stop receiving messages on this sockete.
                    // The Protocol is responsible for closing this.
                    dataInWorkFinished = true;
                    moreMessages = false;
                    
                    synchronized (notify)
                    {
                        notify.notifyAll();
                    }
                }
            };
            dataOut = new BufferedOutputStream(socket.getOutputStream());
        }

        public void expired()
        {
            dispose();
        }
        
        public void dispose()
        {
            releaseSocket();
        }

        @Override
        public void release()
        {
            waitForStreams();
            releaseSocket();
        }

        private void waitForStreams()
        {
            // The Message with the InputStream as a payload can be dispatched
            // into a different thread, in which case we need to wait for it to 
            // finish streaming 
            if (!dataInWorkFinished)
            {
                synchronized (notify)
                {
                    if (!dataInWorkFinished)
                    {
                        try
                        {
                            notify.wait();
                        }
                        catch (InterruptedException e)
                        {
                        }
                    }
                }
            }
        }

        /**
         * Releases the socket when the input stream is closed.
         */
        private void releaseSocket()
        {
            if (socket != null && !socket.isClosed())
            {
                if (logger.isDebugEnabled())
                {
                    // some dirty workaround for IBM JSSE's SSL implementation,
                    // which closes sockets asynchronously by that point.
                    final SocketAddress socketAddress = socket.getLocalSocketAddress();
                    if (socketAddress == null)
                    {
                        logger.debug("Listener has already been closed by other process.");
                    }
                    else
                    {
                        logger.debug("Closing listener: " + socketAddress);
                    }
                }
                    
                try
                {
                    shutdownSocket();
                    socket.close();
                }
                catch (IOException e)
                {
                    logger.warn("Socket close failed with: " + e);
                }
            }
        }

        protected void shutdownSocket() throws IOException
        {
            try
            {
                socket.shutdownOutput();
            }
            catch (UnsupportedOperationException e)
            {
                //Ignore, not supported by ssl sockets
            }
        }

        @Override
        protected void bindTransaction(Transaction tx) throws TransactionException
        {
            //nothing to do
        }

        @Override
        protected Object getNextMessage(Object resource) throws Exception
        {
            long keepAliveTimeout = ((TcpConnector)connector).getKeepAliveTimeout();
            
            Object readMsg = null;
            try
            {
                // Create a monitor if expiry was set
                if(keepAliveTimeout > 0)
                {
                    ((TcpConnector) connector).getKeepAliveMonitor().addExpirable(keepAliveTimeout, 
                        TimeUnit.MILLISECONDS, this);
                }
                
                readMsg = protocol.read(dataIn);
                
                // There was some action so we can clear the monitor
                ((TcpConnector) connector).getKeepAliveMonitor().removeExpirable(this);
                
                if (dataIn.isStreaming())
                {
                    moreMessages = false;
                } 
                
                return readMsg;
            }
            catch (SocketTimeoutException e)
            {
                ((TcpConnector) connector).getKeepAliveMonitor().removeExpirable(this);
            }
            finally
            {
                if (readMsg == null)
                {
                    // Protocols can return a null object, which means we're done
                    // reading messages for now and can mark the stream for closing later.
                    // Also, exceptions can be thrown, in which case we're done reading.
                    dataIn.close();
                }
            }
            
            return null;
        }
        
        @Override
        protected boolean hasMoreMessages(Object message)
        {
            return !socket.isClosed() && !dataInWorkFinished 
                && !disposing.get() && moreMessages;
        }

        @Override
        protected void handleResults(List messages) throws Exception
        {            
            //should send back only if remote synch is set or no outbound endpoints
            if (endpoint.getExchangePattern().hasResponse())
            {
                for (Iterator iterator = messages.iterator(); iterator.hasNext();)
                {
                    Object o = iterator.next();
                    protocol.write(dataOut, o);
                    dataOut.flush();
                }
            }
        }

        @Override
        protected void preRouteMuleMessage(final MuleMessage message) throws Exception
        {
            super.preRouteMuleMessage(message);

            final SocketAddress clientAddress = socket.getRemoteSocketAddress();
            if (clientAddress != null)
            {
                message.setOutboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS, clientAddress.toString());
            }
        }
    }

}
