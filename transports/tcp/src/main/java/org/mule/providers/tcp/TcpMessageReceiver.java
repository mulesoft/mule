/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.tcp;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.ResponseOutputStream;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.AbstractReceiverResourceWorker;
import org.mule.providers.ConnectException;
import org.mule.providers.tcp.i18n.TcpMessages;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.DisposeException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

    public TcpMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
    }

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
            throw new org.mule.providers.ConnectException(TcpMessages.failedToBindToUri(uri), e, this);
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

    protected void doStart() throws UMOException
    {
        // nothing to do
    }

    protected void doStop() throws UMOException
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
                Socket socket = null;
                try
                {
                    socket = serverSocket.accept();

                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Accepted: " + serverSocket);
                    }
                }
                catch (java.io.InterruptedIOException iie)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Interupted IO doing serverSocket.accept: " + iie.getMessage());
                    }
                }
                catch (Exception e)
                {
                    if (!connector.isDisposed() && !disposing.get())
                    {
                        logger.warn("Accept failed on socket: " + e, e);
                        handleException(new ConnectException(e, this));
                    }
                }

                if (socket != null)
                {
                    try
                    {
                        Work work = createWork(socket);
                        try
                        {
                            getWorkManager().scheduleWork(work, WorkManager.INDEFINITE, null, connector);
                        }
                        catch (WorkException e)
                        {
                            logger.error("Tcp Server receiver Work was not processed: " + e.getMessage(), e);
                        }
                    }
                    catch (IOException e)
                    {
                        handleException(e);
                    }
                }
            }
        }
    }

    public void release()
    {
        // template method
    }

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

    protected class TcpWorker extends AbstractReceiverResourceWorker implements Disposable
    {
        protected Socket socket = null;
        protected TcpInputStream dataIn;
        protected InputStream underlyingIn;
        protected OutputStream dataOut;
        protected TcpProtocol protocol;
        protected boolean dataInWorkFinished = false;
        protected Object notify = new Object();
        private boolean moreMessages = true;
        
        public TcpWorker(Object resource, AbstractMessageReceiver receiver) throws IOException
        {
            super(resource, receiver, new ResponseOutputStream((Socket) resource));

            this.socket = (Socket) resource;

            final TcpConnector tcpConnector = ((TcpConnector) connector);
            this.protocol = tcpConnector.getTcpProtocol();

            try
            {
                tcpConnector.configureSocket(TcpConnector.SERVER, socket);

                underlyingIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dataIn = new TcpInputStream(underlyingIn)
                {
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
                dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            }
            catch (IOException e)
            {
                logger.error("Failed to set Socket properties: " + e.getMessage(), e);
            }
        }

        public void dispose()
        {
            releaseSocket();
        }

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
            try
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
                    socket.close();
                }
            }
            catch (IOException e)
            {
                logger.warn("Socket close failed with: " + e);
            }
        }

        protected void bindTransaction(UMOTransaction tx) throws TransactionException
        {
            //nothing to do
        }

        protected Object getNextMessage(Object resource) throws Exception
        {
        	Object readMsg = null;
            try
            {
                readMsg = protocol.read(dataIn);

                if (dataIn.isStreaming())
                {
                    moreMessages = false;
                } 
                
                return readMsg;
            }
            catch (SocketTimeoutException e)
            {
                if (!socket.getKeepAlive())
                {
                    return null;
                }
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
        
        protected boolean hasMoreMessages(Object message)
        {
            return !socket.isClosed() && !dataInWorkFinished 
                && !disposing.get() && moreMessages;
        }

        //@Override
        protected void handleResults(List messages) throws Exception
        {            
            //should send back only if remote synch is set or no outbound endpoints
            if (endpoint.isRemoteSync() || !component.getDescriptor().getOutboundRouter().hasEndpoints())
            {
                for (Iterator iterator = messages.iterator(); iterator.hasNext();)
                {
                    Object o = iterator.next();
                    protocol.write(dataOut, o);
                    dataOut.flush();
                }
            }
        }

        protected Object processData(Object data) throws Exception
        {
            UMOMessageAdapter adapter = connector.getMessageAdapter(data);
            OutputStream os = new ResponseOutputStream(socket);
            UMOMessage returnMessage = routeMessage(new MuleMessage(adapter), endpoint.isSynchronous(), os);
            if (returnMessage != null)
            {
                return returnMessage;
            }
            else
            {
                return null;
            }
        }

    }

}
