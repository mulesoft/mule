/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import org.mule.module.http.internal.listener.ServerAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.FilterChainEvent;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.ssl.SSLFilter;

/**
 * Grizzly filter to delegate to the right SSL Filter based on the Connection.
 *
 * Mule allows to define several listener config, each one associated with a ServerSocket that
 * may have a particular TLS configuration or not use TLS at all. In order to reuse the same grizzly
 * transport we can only have one filter for SSL for every listener config. So this filter keeps
 * record of all the ServerSockets configured and their particular TLS configuration. So once
 * a request arrive it delegates to the right SSL filter based on the connection being processed.
 */
public class GrizzlyHttpsSslDelegateFilter extends BaseFilter
{

    private Map<ServerAddress, SSLFilter> sslFilters = new HashMap<>();

    @Override
    public void onAdded(FilterChain filterChain)
    {
        super.onAdded(filterChain);
    }

    @Override
    public void onFilterChainChanged(FilterChain filterChain)
    {
        super.onFilterChainChanged(filterChain);
    }

    @Override
    public void onRemoved(FilterChain filterChain)
    {
        super.onRemoved(filterChain);
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException
    {
        SSLFilter sslFilter = retrieveSslFilter(ctx.getConnection());
        if (sslFilter != null)
        {
            return sslFilter.handleRead(ctx);
        }
        else
        {
            return super.handleRead(ctx);
        }
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException
    {
        SSLFilter sslFilter = retrieveSslFilter(ctx.getConnection());
        if (sslFilter != null)
        {
            return sslFilter.handleWrite(ctx);
        }
        else
        {
            return super.handleWrite(ctx);
        }
    }

    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException
    {
        SSLFilter sslFilter = retrieveSslFilter(ctx.getConnection());
        if (sslFilter != null)
        {
            return sslFilter.handleConnect(ctx);
        }
        else
        {
            return super.handleConnect(ctx);
        }
    }

    @Override
    public NextAction handleAccept(FilterChainContext ctx) throws IOException
    {
        SSLFilter sslFilter = retrieveSslFilter(ctx.getConnection());
        if (sslFilter != null)
        {
            return sslFilter.handleAccept(ctx);
        }
        else
        {
            return super.handleAccept(ctx);
        }
    }

    @Override
    public NextAction handleEvent(FilterChainContext ctx, FilterChainEvent event) throws IOException
    {
        SSLFilter sslFilter = retrieveSslFilter(ctx.getConnection());
        if (sslFilter != null)
        {
            return sslFilter.handleEvent(ctx, event);
        }
        else
        {
            return super.handleEvent(ctx, event);
        }
    }

    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException
    {
        SSLFilter sslFilter = retrieveSslFilter(ctx.getConnection());
        if (sslFilter != null)
        {
            return sslFilter.handleClose(ctx);
        }
        else
        {
            return super.handleClose(ctx);
        }
    }

    @Override
    public void exceptionOccurred(FilterChainContext ctx, Throwable error)
    {
        SSLFilter sslFilter = retrieveSslFilter(ctx.getConnection());
        if (sslFilter != null)
        {
            sslFilter.exceptionOccurred(ctx, error);
        }
        else
        {
            super.exceptionOccurred(ctx, error);
        }
    }

    @Override
    public FilterChainContext createContext(Connection connection, FilterChainContext.Operation operation)
    {
        SSLFilter sslFilter = retrieveSslFilter(connection);
        if (sslFilter != null)
        {
            return sslFilter.createContext(connection, operation);
        }
        else
        {
            return super.createContext(connection, operation);
        }
    }


    private SSLFilter retrieveSslFilter(Connection connection)
    {
        final InetSocketAddress inetAddress = (InetSocketAddress) connection.getLocalAddress();
        final int port = inetAddress.getPort();
        final String host = inetAddress.getHostName();
        return sslFilters.get(new ServerAddress(host, port));
    }

    /**
     * Adds a new SSL Filter for a particular Server address
     *
     * @param serverAddress the server address to which this SSL filter must be applied
     * @param sslFilter the ssl filter to apply
     */
    public synchronized void addSslFilterForAddress(ServerAddress serverAddress, SSLFilter sslFilter)
    {
        this.sslFilters.put(serverAddress, sslFilter);
    }
}
