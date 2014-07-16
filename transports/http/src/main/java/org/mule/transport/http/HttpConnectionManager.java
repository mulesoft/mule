/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.MuleRuntimeException;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointURI;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manager {@link HttpRequestDispatcher} connections and disconnections to {@link EndpointURI}.
 * <p/>
 * Starts listening for HTTP request when at least one endpoint is associated to a given port are connected and
 * stops listening for HTTP request when all endpoints associated to a given port are disconnected.
 */
class HttpConnectionManager
{

    private static final int LAST_CONNECTION = 1;
    protected final Log logger = LogFactory.getLog(getClass());
    final private HttpConnector connector;
    final private Map<String, HttpRequestDispatcher> socketDispatchers = new HashMap<String, HttpRequestDispatcher>();
    final private Map<String, Integer> socketDispatcherCount = new HashMap<String, Integer>();
    final private WorkManager workManager;

    public HttpConnectionManager(HttpConnector connector, WorkManager workManager)
    {
        if (connector == null)
        {
            throw new IllegalArgumentException("HttpConnector can not be null");
        }
        if (workManager == null)
        {
            throw new IllegalArgumentException("WorkManager can not be null");
        }
        this.connector = connector;
        this.workManager = workManager;
    }

    synchronized void addConnection(final EndpointURI endpointURI)
    {
        try
        {
            String endpointKey = getKeyForEndpointUri(endpointURI);
            if (socketDispatchers.containsKey(endpointKey))
            {
                socketDispatcherCount.put(endpointKey, socketDispatcherCount.get(endpointKey) + 1);
            }
            else
            {
                ServerSocket serverSocket = connector.getServerSocket(endpointURI.getUri());
                HttpRequestDispatcher httpRequestDispatcher = new HttpRequestDispatcher(connector, connector.getRetryPolicyTemplate(), serverSocket, workManager);
                socketDispatchers.put(endpointKey, httpRequestDispatcher);
                socketDispatcherCount.put(endpointKey, new Integer(1));
                workManager.scheduleWork(httpRequestDispatcher, WorkManager.INDEFINITE, null, connector);
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    synchronized void removeConnection(final EndpointURI endpointURI)
    {
        String endpointKey = getKeyForEndpointUri(endpointURI);
        if (!socketDispatchers.containsKey(endpointKey))
        {
            logger.warn("Trying to disconnect endpoint with uri " + endpointKey + " but " + HttpRequestDispatcher.class.getName() + " does not exists for that uri");
            return;
        }
        Integer connectionsRequested = socketDispatcherCount.get(endpointKey);
        if (connectionsRequested == LAST_CONNECTION)
        {
            HttpRequestDispatcher httpRequestDispatcher = socketDispatchers.get(endpointKey);
            httpRequestDispatcher.disconnect();
            socketDispatchers.remove(endpointKey);
            socketDispatcherCount.remove(endpointKey);
        }
        else
        {
            socketDispatcherCount.put(endpointKey, socketDispatcherCount.get(endpointKey) - 1);
        }
    }

    private String getKeyForEndpointUri(final EndpointURI endpointURI)
    {
        return endpointURI.getHost() + ":" + endpointURI.getPort();
    }

    public void dispose()
    {
        for (HttpRequestDispatcher httpRequestDispatcher : socketDispatchers.values())
        {
            httpRequestDispatcher.disconnect();
        }
        socketDispatchers.clear();
        socketDispatcherCount.clear();
        workManager.dispose();
    }
}
