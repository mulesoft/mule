/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.config.MutableThreadingProfile;
import org.mule.transport.ConnectException;
import org.mule.util.concurrent.ThreadNameHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manage a ServerSocket.
 * <p/>
 * Lookup the right MessageReceiver for each HttpRequest and dispatch the socket to the MessageReceiver for further processing.
 */
class HttpRequestDispatcher implements Work
{

    private static Log logger = LogFactory.getLog(HttpRequestDispatcher.class);

    private ServerSocket serverSocket;
    private HttpConnector httpConnector;
    private RetryPolicyTemplate retryTemplate;
    protected ExecutorService requestHandOffExecutor;
    private WorkManager workManager;
    private final AtomicBoolean disconnect = new AtomicBoolean(false);

    public HttpRequestDispatcher(final HttpConnector httpConnector, final RetryPolicyTemplate retryPolicyTemplate, final ServerSocket serverSocket, final WorkManager workManager)
    {
        if (httpConnector == null)
        {
            throw new IllegalArgumentException("HttpConnector can not be null");
        }
        if (retryPolicyTemplate == null)
        {
            throw new IllegalArgumentException("RetryPolicyTemplate can not be null");
        }
        if (serverSocket == null)
        {
            throw new IllegalArgumentException("ServerSocket can not be null");
        }
        if (workManager == null)
        {
            throw new IllegalArgumentException("WorkManager can not be null");
        }
        this.httpConnector = httpConnector;
        this.retryTemplate = retryPolicyTemplate;
        this.serverSocket = serverSocket;
        this.workManager = workManager;
        this.requestHandOffExecutor = createRequestDispatcherThreadPool(httpConnector);
    }

    private ExecutorService createRequestDispatcherThreadPool(HttpConnector httpConnector)
    {
        ThreadingProfile receiverThreadingProfile = httpConnector.getReceiverThreadingProfile();
        MutableThreadingProfile dispatcherThreadingProfile = new MutableThreadingProfile(receiverThreadingProfile);
        dispatcherThreadingProfile.setThreadFactory(null);
        dispatcherThreadingProfile.setMaxThreadsActive(dispatcherThreadingProfile.getMaxThreadsActive() * 2);
        String threadNamePrefix = ThreadNameHelper.getPrefix(httpConnector.getMuleContext()) + "http.request.dispatch." + serverSocket.getLocalPort();
        ExecutorService executorService = dispatcherThreadingProfile.createPool(threadNamePrefix);
        return executorService;
    }

    @Override
    public void run()
    {
        while (!disconnect.get())
        {
            if (httpConnector.isStarted() && !disconnect.get())
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
                                if (!httpConnector.isDisposed() && !disconnect.get())
                                {
                                    throw new ConnectException(e, null);
                                }
                            }

                            if (socket != null)
                            {
                                final Runnable httpRequestDispatcherWork = new HttpRequestDispatcherWork(httpConnector, socket);
                                // Process each connection in a different thread so we can continue accepting connection right away.
                                requestHandOffExecutor.execute(httpRequestDispatcherWork);
                            }
                        }

                        public String getWorkDescription()
                        {
                            String hostName = ((InetSocketAddress) serverSocket.getLocalSocketAddress()).getHostName();
                            int port = ((InetSocketAddress) serverSocket.getLocalSocketAddress()).getPort();
                            return String.format("%s://%s:%d", httpConnector.getProtocol(), hostName, port);
                        }

                        @Override
                        public Connector getWorkOwner()
                        {
                            return httpConnector;
                        }
                    }, workManager);
                }
                catch (Exception e)
                {
                    httpConnector.getMuleContext().getExceptionListener().handleException(e);
                }
            }
        }
    }

    @Override
    public void release()
    {

    }

    void disconnect()
    {
        disconnect.set(true);
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
        finally
        {
            requestHandOffExecutor.shutdown();
        }

    }

}
