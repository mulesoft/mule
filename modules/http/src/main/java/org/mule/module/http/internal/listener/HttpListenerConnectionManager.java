/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;


import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.internal.listener.grizzly.GrizzlyServerManager;
import org.mule.transport.ssl.TlsContextFactory;
import org.mule.transport.tcp.DefaultTcpServerSocketProperties;
import org.mule.transport.tcp.TcpServerSocketProperties;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.ThreadNameHelper;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.util.Collection;

public class HttpListenerConnectionManager implements Initialisable, Disposable, MuleContextAware
{

    public static final String HTTP_LISTENER_CONNECTION_MANAGER = "_httpListenerConnectionManager";
    private static final String UNKNOWN_APP_NAME = "UNKNOWN-APP";

    private HttpListenerRegistry httpListenerRegistry = new HttpListenerRegistry();
    private HttpServerManager httpServerManager;

    private MuleContext muleContext;

    @Override
    public void initialise() throws InitialisationException
    {
        Collection<TcpServerSocketProperties> tcpServerSocketPropertiesesBeans = muleContext.getRegistry().lookupObjects(TcpServerSocketProperties.class);
        TcpServerSocketProperties tcpServerSocketProperties = new DefaultTcpServerSocketProperties();

        if (tcpServerSocketPropertiesesBeans.size() == 1)
        {
            tcpServerSocketProperties = Iterables.getOnlyElement(tcpServerSocketPropertiesesBeans);
        }
        else if (tcpServerSocketPropertiesesBeans.size() > 1)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("Only one global TCP server socket properties bean should be defined in the config"), this);
        }

        try
        {
            httpServerManager = new GrizzlyServerManager(getAppName(), httpListenerRegistry, tcpServerSocketProperties);
        }
        catch (IOException e)
        {
            throw new InitialisationException(e, this);
        }

    }

    private String getAppName()
    {
        String appName = ThreadNameHelper.getPrefix(muleContext);
        return StringUtils.isEmpty(appName) ? UNKNOWN_APP_NAME : appName;
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

    public Server createServer(ServerAddress serverAddress, boolean usePersistentConnections, int connectionIdleTimeout)
    {
        if (!httpServerManager.containsServerFor(serverAddress))
        {
            try
            {
                return httpServerManager.createServerFor(serverAddress, usePersistentConnections, connectionIdleTimeout);
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        else
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("A server for host(%s) and port(%s) already exists", serverAddress.getHost(), serverAddress.getPort())));
        }
    }

    public Server createSslServer(ServerAddress serverAddress, TlsContextFactory tlsContext, boolean usePersistentConnections, int connectionIdleTimeout)
    {
        if (!httpServerManager.containsServerFor(serverAddress))
        {
            try
            {
                return httpServerManager.createSslServerFor(tlsContext, serverAddress, usePersistentConnections, connectionIdleTimeout);
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        else
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("A server for host(%s) and port(%s) already exists", serverAddress.getHost(), serverAddress.getPort())));
        }
    }

}
