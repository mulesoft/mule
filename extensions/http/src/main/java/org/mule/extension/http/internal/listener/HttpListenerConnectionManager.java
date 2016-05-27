/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;


import static java.lang.String.format;
import static org.mule.runtime.core.api.config.ThreadingProfile.DEFAULT_THREADING_PROFILE;
import org.mule.extension.http.internal.listener.grizzly.GrizzlyServerManager;
import org.mule.extension.http.internal.listener.server.HttpServerConfiguration;
import org.mule.extension.http.internal.listener.server.HttpServerFactory;
import org.mule.module.socket.api.TcpServerSocketProperties;
import org.mule.module.socket.internal.DefaultTcpServerSocketProperties;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.MutableThreadingProfile;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.NetworkUtils;
import org.mule.runtime.core.util.concurrent.ThreadNameHelper;
import org.mule.runtime.module.http.internal.listener.HttpListenerRegistry;
import org.mule.runtime.module.http.internal.listener.HttpServerManager;
import org.mule.runtime.module.http.internal.listener.Server;
import org.mule.runtime.module.http.internal.listener.ServerAddress;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Grizzly based {@link HttpServerFactory}.
 *
 * @since 4.0
 */
public class HttpListenerConnectionManager implements HttpServerFactory, Initialisable, Disposable, MuleContextAware
{

    public static final String HTTP_LISTENER_CONNECTION_MANAGER = "_httpExtListenerConnectionManager";
    public static final String SERVER_ALREADY_EXISTS_FORMAT = "A server in port(%s) already exists for ip(%s) or one overlapping it (0.0.0.0).";
    private static final String LISTENER_THREAD_NAME_PREFIX = "http.listener";
    private static final int DEFAULT_MAX_THREADS = 128;

    private HttpListenerRegistry httpListenerRegistry = new HttpListenerRegistry();
    private ThreadingProfile workerThreadingProfile;
    private HttpServerManager httpServerManager;

    private MuleContext muleContext;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void initialise() throws InitialisationException
    {
        if (initialized.getAndSet(true))
        {
            return;
        }

        Collection<TcpServerSocketProperties> tcpServerSocketPropertiesBeans = muleContext.getRegistry().lookupObjects(TcpServerSocketProperties.class);
        TcpServerSocketProperties tcpServerSocketProperties = new DefaultTcpServerSocketProperties();

        if (tcpServerSocketPropertiesBeans.size() == 1)
        {
            tcpServerSocketProperties = Iterables.getOnlyElement(tcpServerSocketPropertiesBeans);
        }
        else if (tcpServerSocketPropertiesBeans.size() > 1)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("Only one global TCP server socket properties bean should be defined in the config"), this);
        }

        String threadNamePrefix = ThreadNameHelper.getPrefix(muleContext) + LISTENER_THREAD_NAME_PREFIX;
        try
        {
            httpServerManager = new GrizzlyServerManager(threadNamePrefix, httpListenerRegistry, tcpServerSocketProperties);
        }
        catch (IOException e)
        {
            throw new InitialisationException(e, this);
        }

        //TODO: MULE-9320 Define threading model for message sources in Mule 4 - Analyse whether this can be avoided
        workerThreadingProfile = new MutableThreadingProfile(DEFAULT_THREADING_PROFILE);
        workerThreadingProfile.setMaxThreadsActive(DEFAULT_MAX_THREADS);

    }

    @Override
    public synchronized void dispose()
    {
        httpServerManager.dispose();
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Server create(HttpServerConfiguration serverConfiguration) throws ConnectionException
    {
        ServerAddress serverAddress;
        String host = serverConfiguration.getHost();
        try
        {
            serverAddress = createServerAddress(host, serverConfiguration.getPort());
        }
        catch (UnknownHostException e)
        {
            throw new ConnectionException(String.format("Cannot resolve host %s", host), e);
        }

        //TODO: Should save a reference to this so as to dispose it later
        WorkManager workManager = createWorkManager(serverConfiguration.getOwnerName());
        try
        {
            workManager.start();
        }
        catch (MuleException e)
        {
            throw new ConnectionException("Could not create work manager", e);
        }

        TlsContextFactory tlsContextFactory = serverConfiguration.getTlsContextFactory();
        if (tlsContextFactory == null)
        {
            return createServer(serverAddress,
                                createWorkManagerSource(workManager),
                                serverConfiguration.isUsePersistentConnections(),
                                serverConfiguration.getConnectionIdleTimeout());
        }
        else
        {
            return createSslServer(serverAddress,
                                   createWorkManagerSource(workManager),
                                   tlsContextFactory,
                                   serverConfiguration.isUsePersistentConnections(),
                                   serverConfiguration.getConnectionIdleTimeout());
        }
    }

    public Server createServer(ServerAddress serverAddress, WorkManagerSource workManagerSource, boolean usePersistentConnections, int connectionIdleTimeout)
    {
        if (!containsServerFor(serverAddress))
        {
            try
            {
                return httpServerManager.createServerFor(serverAddress, workManagerSource, usePersistentConnections, connectionIdleTimeout);
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        else
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp())));
        }
    }

    public boolean containsServerFor(ServerAddress serverAddress)
    {
        return httpServerManager.containsServerFor(serverAddress);
    }

    public Server createSslServer(ServerAddress serverAddress, WorkManagerSource workManagerSource, TlsContextFactory tlsContext, boolean usePersistentConnections, int connectionIdleTimeout)
    {
        if (!containsServerFor(serverAddress))
        {
            try
            {
                return httpServerManager.createSslServerFor(tlsContext, workManagerSource, serverAddress, usePersistentConnections, connectionIdleTimeout);
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        else
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp())));
        }
    }

    /**
     * Creates the server address object with the IP and port that a server should bind to.
     */
    private ServerAddress createServerAddress(String host, int port) throws UnknownHostException
    {
        return new ServerAddress(NetworkUtils.getLocalHostIp(host), port);
    }

    private WorkManager createWorkManager(String name)
    {
        final WorkManager workManager = workerThreadingProfile.createWorkManager(format("%s%s.%s", ThreadNameHelper.getPrefix(muleContext), name, "worker"), muleContext.getConfiguration().getShutdownTimeout());
        if (workManager instanceof MuleContextAware)
        {
            ((MuleContextAware) workManager).setMuleContext(muleContext);
        }
        return workManager;
    }

    private WorkManagerSource createWorkManagerSource(WorkManager workManager)
    {
        return () -> workManager;
    }
}
