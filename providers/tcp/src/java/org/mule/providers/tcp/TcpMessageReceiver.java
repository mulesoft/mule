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

 *

 */
package org.mule.providers.tcp;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;
import org.mule.DisposeException;
import org.mule.InitialisationException;
import org.mule.impl.MuleMessage;
import org.mule.impl.ResponseOutputStream;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.provider.UMOMessageAdapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;

/**
 * <code>TcpMessageReceiver</code> acts like a tcp server to receive socket
 * requests.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TcpMessageReceiver extends AbstractMessageReceiver implements Runnable
{
    private ServerSocket serverSocket = null;

    private PooledExecutor threadPool;

    private Thread worker;

    public TcpMessageReceiver(AbstractConnector connector,
                              UMOComponent component,
                              UMOEndpoint endpoint) throws InitialisationException
    {
        create(connector, component, endpoint);
        connector.getReceiverThreadingProfile().setThreadFactory(getThreadFactory());
        threadPool = connector.getReceiverThreadingProfile().createPool(connector.getName());
        URI uri = endpoint.getEndpointURI().getUri();

        connect(uri);
        worker = new Thread(this, connector.getName() + ".receiver");
        worker.start();
    }

    protected ThreadFactory getThreadFactory() {
            return new ThreadFactory() {
            public Thread newThread(Runnable runnable)
            {
                Thread thread = new Thread(runnable, toString());
                if (isServerSide()) {
                    thread.setDaemon(true);
                }
                else {
                    thread.setPriority(Thread.NORM_PRIORITY + 2);
                }
                return thread;
            }
        };
    }

    protected void connect(URI uri) throws InitialisationException
    {
        int count = ((TcpConnector) connector).getRetryCount();
        long freq = ((TcpConnector) connector).getRetryFrequency();
        count++;
        for (int i = 0; i < count; i++)
        {
            try
            {
                serverSocket = createSocket(uri);

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

    protected ServerSocket createSocket(URI uri) throws Exception
    {
        String host = uri.getHost();
        InetAddress inetAddress = null;
        int backlog = ((TcpConnector) connector).getBacklog();
        if (host == null || host.length() == 0)
        {
            host = "localhost";
        }
        inetAddress = InetAddress.getByName(host);
        if (inetAddress.equals(InetAddress.getLocalHost()) || inetAddress.isLoopbackAddress() ||
                host.trim().equals("localhost"))
        {
            return new ServerSocket(uri.getPort(), backlog);
        } else
        {
            return new ServerSocket(uri.getPort(), backlog, inetAddress);
        }
    }

    /**
     * Obtain the serverSocket
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
                    logger.trace("Server socket Accepted on: " + serverSocket.getLocalPort());
                } catch (java.io.InterruptedIOException iie)
                {
                    logger.debug("Interupted IO doing serverSocket.accept: " + iie.getMessage());
                } catch (Exception e)
                {
                    if (!connector.isDisposed() && !disposing.get())
                    {
                        logger.warn("Accept failed on socket: " + e, e);
                        handleException(null, e);
                    }
                }
                if (socket != null)
                {
                    Runnable worker = createWorker(socket);
                    try
                    {
                        threadPool.execute(worker);
                    } catch (InterruptedException e)
                    {
                        logger.error("Tcp Server receiver interrupted: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    public void doDispose() throws UMOException
    {
        if (worker != null)
        {
            worker.interrupt();
            worker = null;
        }
        try
        {
            threadPool.shutdownNow();

            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            serverSocket = null;

        } catch (Exception e)
        {
            throw new DisposeException("Failed to close http port: " + e.getMessage(), e);
        }
        logger.info("Closed Tcp port");
    }

    protected Runnable createWorker(Socket socket)
    {
        return new TcpWorker(socket);
    }


    protected class TcpWorker implements Runnable, Disposable
    {
        protected Socket socket = null;
        protected DataInputStream dataIn;
        protected DataOutputStream dataOut;
        protected SynchronizedBoolean closed = new SynchronizedBoolean(false);

        public TcpWorker(Socket socket)
        {
            this.socket = socket;
        }

        public void dispose()
        {
            closed.set(true);
            try
            {
                if (socket != null) {
                    logger.debug("Closing listener: " + socket.getInetAddress());
                    socket.close();
                }
            } catch (IOException e)
            {
                logger.error("Socket close failed with: " + e);
            }
        }

        /**
         * Accept requests from a given TCP port
         */
        public void run()
        {
            try
            {
                dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                int counter=0;
                while (!socket.isClosed() && !disposing.get())
                {
                    if (isServerSide() && ++counter > 500) {
                        counter = 0;
                        Thread.yield();
                    }

                    byte[] b = readStream(dataIn);
                    //end of stream
                    if (b == null) break;

                    byte[] result = processData(b);
                    if(result!=null) {
                        dataOut.write(result);
                    }
                    dataOut.flush();
                }
            } catch (Exception e)
            {
                handleException("Failed to process tcp Request on: "
                        + (socket != null ? socket.getInetAddress().toString() : "null"),
                        e);
            } finally {
                dispose();
            }
        }

        protected byte[] processData(byte[] data) throws Exception
        {
            UMOMessageAdapter adapter = connector.getMessageAdapter(data);
            OutputStream os = new ResponseOutputStream(socket.getOutputStream(), socket);
            UMOMessage returnMessage = routeMessage(new MuleMessage(adapter), connector.isSynchronous(), os);
            if (returnMessage != null)
            {
                return returnMessage.getPayloadAsBytes();
            } else {
                return null;
            }
        }

        private byte[] readStream(InputStream is) throws IOException
        {
            ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            byte[] buffer = new byte[((TcpConnector)connector).getBufferSize()];
            int len = 0;
            try
            {
                while ((len = is.read(buffer)) == 0)
                {
                }
            } catch (SocketException e)
            {
                logger.warn(e.getMessage());
                return null;
            }
            if (len == -1)
            {
                logger.info("The socket peer closed");
                return null;
            } else
            {
                do
                {
                    baos.write(buffer, 0, len);
                    if (len < buffer.length) break;
                } while ((len = is.read(buffer)) > 0);

                baos.flush();
                baos.close();
                return baos.toByteArray();
            }
        }
    }
}