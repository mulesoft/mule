/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.MutableThreadingProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.tcp.DefaultTcpServerSocketProperties;
import org.mule.transport.tcp.TcpServerSocketProperties;
import org.mule.util.NetworkUtils;
import org.mule.util.Preconditions;
import org.mule.util.concurrent.ThreadNameHelper;

import java.io.IOException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHttpListenerConfig implements HttpListenerConfig, Initialisable, MuleContextAware
{

    public static final int DEFAULT_MAX_THREADS = 128;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 30 * 1000;

    private String name;
    private String host;
    private int port;
    private String basePath;
    private Boolean parseRequest;
    private MuleContext muleContext;
    private HttpListenerConnectionManager connectionManager;
    private TlsContextFactory tlsContext;
    private TcpServerSocketProperties serverSocketProperties = new DefaultTcpServerSocketProperties();
    private ThreadingProfile workerThreadingProfile;
    private boolean started = false;
    private Server server;
    private WorkManager workManager;
    private boolean initialised;

    private boolean usePersistentConnections = true;
    private int connectionIdleTimeout = DEFAULT_CONNECTION_IDLE_TIMEOUT;

    public void setWorkerThreadingProfile(ThreadingProfile workerThreadingProfile)
    {
        this.workerThreadingProfile = workerThreadingProfile;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }

    public void setTlsContext(TlsContextFactory tlsContext)
    {
        this.tlsContext = tlsContext;
    }

    public void setServerSocketProperties(TcpServerSocketProperties serverSocketProperties)
    {
        this.serverSocketProperties = serverSocketProperties;
    }

    public void setParseRequest(Boolean parseRequest)
    {
        this.parseRequest = parseRequest;
    }

    public String resolvePath(String listenerPath)
    {
        Preconditions.checkArgument(listenerPath.startsWith("/"), "listenerPath must start with /");
        return this.basePath == null ? listenerPath : this.basePath + listenerPath;
    }

    @Override
    public synchronized void initialise() throws InitialisationException
    {
        if (initialised)
        {
            return;
        }
        basePath = HttpParser.sanitizePathWithStartSlash(this.basePath);
        try
        {
            connectionManager = muleContext.getRegistry().lookupObject(HttpListenerConnectionManager.class);
        }
        catch (RegistrationException e)
        {
            throw new InitialisationException(e, this);
        }
        if (workerThreadingProfile == null)
        {
            workerThreadingProfile = new MutableThreadingProfile(ThreadingProfile.DEFAULT_THREADING_PROFILE);
            workerThreadingProfile.setMaxThreadsActive(DEFAULT_MAX_THREADS);
        }
        if (tlsContext != null && !tlsContext.isKeyStoreConfigured())
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("KeyStore must be configured for server side SSL"), this);
        }

        verifyConnectionsParameters();


        ServerAddress serverAddress;

        try
        {
            serverAddress = createServerAddress();
        }
        catch (UnknownHostException e)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("Cannot resolve host %s", host), e, this);
        }

        if (tlsContext == null)
        {
            server = connectionManager.createServer(serverAddress, createWorkManagerSource(), usePersistentConnections, connectionIdleTimeout);
        }
        else
        {
            server = connectionManager.createSslServer(serverAddress, createWorkManagerSource(), tlsContext, usePersistentConnections, connectionIdleTimeout);
        }
        initialised = true;
    }

    //We use a WorkManagerSource since the workManager instance may be recreated during stop/start and it would leave the server with an invalid work manager instance.
    private WorkManagerSource createWorkManagerSource()
    {
        return new WorkManagerSource()
        {
            @Override
            public WorkManager getWorkManager() throws MuleException
            {
                return workManager;
            }
        };
    }

    private void verifyConnectionsParameters() throws InitialisationException
    {
        if (!usePersistentConnections)
        {
            connectionIdleTimeout = 0;
        }
    }

    private WorkManager createWorkManager()
    {
        final WorkManager workManager = workerThreadingProfile.createWorkManager(String.format("%s%s.%s", ThreadNameHelper.getPrefix(muleContext), name, "worker"), muleContext.getConfiguration().getShutdownTimeout());
        if (workManager instanceof MuleContextAware)
        {
            ((MuleContextAware) workManager).setMuleContext(muleContext);
        }
        return workManager;
    }

    /**
     * Creates the server address object with the IP and port that this config should bind to.
     */
    private ServerAddress createServerAddress() throws UnknownHostException
    {
        return new ServerAddress(NetworkUtils.getLocalHostIp(host), port);
    }

    public void setMuleContext(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public RequestHandlerManager addRequestHandler(ListenerRequestMatcher requestMatcher, RequestHandler requestHandler) throws IOException
    {
        return server.addRequestHandler(requestMatcher, requestHandler);
    }

    public Boolean resolveParseRequest(Boolean listenerParseRequest)
    {
        return listenerParseRequest != null ? listenerParseRequest : (parseRequest != null ? parseRequest : true);
    }

    public int getPort()
    {
        return port;
    }

    public String getHost()
    {
        return host;
    }

    @Override
    public TlsContextFactory getTlsContext()
    {
        return tlsContext;
    }

    @Override
    public synchronized void start() throws MuleException
    {
        if (started)
        {
            return;
        }
        try
        {
            workManager = createWorkManager();
            workManager.start();
            server.start();
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
        started = true;
    }

    @Override
    public boolean hasTlsConfig()
    {
        return this.tlsContext != null;
    }

    @Override
    public synchronized void stop() throws MuleException
    {
        if (started)
        {
            try
            {
                workManager.dispose();
            }
            catch (Exception e)
            {
                logger.warn("Failure shutting down work manager " + e.getMessage());
                if (logger.isDebugEnabled())
                {
                    logger.debug(e.getMessage(), e);
                }
            }
            finally
            {
                workManager = null;
            }
            server.stop();
            started = false;
        }
    }

    public String getName()
    {
        return name;
    }

    WorkManager getWorkManager()
    {
        return workManager;
    }

    public void setUsePersistentConnections(boolean usePersistentConnections)
    {
        this.usePersistentConnections = usePersistentConnections;
    }

    public void setConnectionIdleTimeout(int connectionIdleTimeout)
    {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

}
