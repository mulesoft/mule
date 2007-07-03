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

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.ResponseOutputStream;
import org.mule.impl.model.streaming.CloseCountDownInputStream;
import org.mule.impl.model.streaming.CloseCountDownOutputStream;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.providers.tcp.i18n.TcpMessages;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;


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
     *
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
            // TODO why is this spin loop here? It's not there on the trunk.
            //try
            //{
            //    Thread.sleep(100);
            //}
            //catch (InterruptedException e)
            //{
            //    break;
            //}
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
        return new TcpWorker(socket);
    }

    protected class TcpWorker implements Work, Disposable
    {
        protected Socket socket = null;
        protected InputStream dataIn;
        protected OutputStream dataOut;
        protected AtomicBoolean closed = new AtomicBoolean(false);
        protected TcpProtocol protocol;

        public TcpWorker(Socket socket)
        {
            final TcpConnector tcpConnector = (TcpConnector) connector;
            this.socket = socket;
            this.protocol = tcpConnector.getTcpProtocol();

            try
            {
                tcpConnector.configureSocket(TcpConnector.SERVER, socket);
            }
            catch (SocketException e)
            {
                logger.error("Failed to set Socket properties: " + e.getMessage(), e);
            }
        }

        public void release()
        {
            dispose();
        }

        public void dispose()
        {
            closed.set(true);
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

        /** Accept requests from a given TCP port */
        public void run()
        {
            try
            {
                dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                if (endpoint.isStreaming())
                {
                    // all we can do for streaming is connect the streams
                    CountDownLatch latch;
                    if (endpoint.isSynchronous())
                    {
                        latch = new CountDownLatch(2);
                        dataOut = new CloseCountDownOutputStream(dataOut, latch);
                    }
                    else
                    {
                        latch = new CountDownLatch(2);
                    }
                    dataIn = new CloseCountDownInputStream(dataIn, latch);

                    UMOMessageAdapter adapter = connector.getStreamMessageAdapter(dataIn, dataOut);
                    routeMessage(new MuleMessage(adapter), endpoint.isSynchronous(), null);

                    latch.await();
                }
                else
                {
                    while (!socket.isClosed() && !disposing.get())
                    {
                        try
                        {
                            Object readMsg = protocol.read(dataIn);
                            if (readMsg == null)
                            {
                                break;
                            }
                            
                            boolean remoteSync = endpoint.isRemoteSync();
                            Object result = processData(readMsg);
                            
                            //should send back only if rem synch or no outbound endpoints
                            if (result != null && (remoteSync || !component.getDescriptor().getOutboundRouter().hasEndpoints()))
                            {
                                protocol.write(dataOut, result);
                                dataOut.flush();
                            }

                            
                        }
                        catch (SocketTimeoutException e)
                        {
                            if (!socket.getKeepAlive())
                            {
                                break;
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                handleException(e);
            }
            finally
            {
                dispose();
            }
        }

        protected Object processData(Object data) throws Exception
        {
            UMOMessageAdapter adapter = connector.getMessageAdapter(data);
            OutputStream os = new ResponseOutputStream(socket.getOutputStream(), socket);
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

    protected class TcpStreamWorker extends TcpWorker
    {

        public TcpStreamWorker(Socket socket)
        {
            super(socket);
        }

        public void run()
        {
            super.run();
        }
    }

}
