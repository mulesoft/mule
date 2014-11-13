/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mule.module.http.internal.listener.HttpListenerConnectionManager.HTTP_LISTENER_CONNECTION_MANAGER;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.MutableThreadingProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.transport.ssl.TlsContextFactory;
import org.mule.transport.tcp.DefaultTcpServerSocketProperties;
import org.mule.transport.tcp.TcpServerSocketProperties;
import org.mule.util.Preconditions;
import org.mule.util.concurrent.ThreadNameHelper;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHttpListenerConfig implements HttpListenerConfig, Initialisable, MuleContextAware, Startable, Stoppable
{

    public static final int DEFAULT_MAX_THREADS = 128;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String name;
    private String host;
    private Integer port;
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
        connectionManager = muleContext.getRegistry().get(HTTP_LISTENER_CONNECTION_MANAGER);
        if (workerThreadingProfile == null)
        {
            workerThreadingProfile = new MutableThreadingProfile(ThreadingProfile.DEFAULT_THREADING_PROFILE);
            workerThreadingProfile.setMaxThreadsActive(DEFAULT_MAX_THREADS);
        }
        if (tlsContext != null && !tlsContext.isKeyStoreConfigured())
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("KeyStore must be configured for server side SSL"), this);
        }
        if (tlsContext == null)
        {
            server = connectionManager.createServer(host, port);
        }
        else
        {
            server = connectionManager.createSslServer(host, port, tlsContext);
        }
        initialised = true;
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

    public Integer getPort()
    {
        return port;
    }

    public String getHost()
    {
        return host;
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

}
