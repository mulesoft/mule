/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import org.mule.api.context.WorkManager;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.tcp.TcpClientSocketProperties;


public class GrizzlyHttpClientConfiguration
{
    private final TlsContextFactory tlsContextFactory;
    private final ProxyConfig proxyConfig;
    private final TcpClientSocketProperties clientSocketProperties;
    private final int maxConnections;
    private final boolean usePersistentConnections;
    private final int connectionIdleTimeout;
    private final String threadNamePrefix;
    private final WorkManager workManager;

    private GrizzlyHttpClientConfiguration(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, TcpClientSocketProperties clientSocketProperties,
                                           int maxConnections, boolean usePersistentConnections, int connectionIdleTimeout, String threadNamePrefix, WorkManager workManager)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.clientSocketProperties = clientSocketProperties;
        this.maxConnections = maxConnections;
        this.usePersistentConnections = usePersistentConnections;
        this.connectionIdleTimeout = connectionIdleTimeout;
        this.threadNamePrefix = threadNamePrefix;
        this.workManager = workManager;
    }

    public TlsContextFactory getTlsContextFactory()
    {
        return tlsContextFactory;
    }

    public ProxyConfig getProxyConfig()
    {
        return proxyConfig;
    }

    public TcpClientSocketProperties getClientSocketProperties()
    {
        return clientSocketProperties;
    }

    public int getMaxConnections()
    {
        return maxConnections;
    }

    public boolean isUsePersistentConnections()
    {
        return usePersistentConnections;
    }

    public int getConnectionIdleTimeout()
    {
        return connectionIdleTimeout;
    }

    public String getThreadNamePrefix()
    {
        return threadNamePrefix;
    }

    public WorkManager getWorkManager()
    {
        return workManager;
    }

    public static class Builder
    {
        private TlsContextFactory tlsContextFactory;
        private ProxyConfig proxyConfig;
        private TcpClientSocketProperties clientSocketProperties;
        private int maxConnections;
        private boolean usePersistentConnections;
        private int connectionIdleTimeout;
        private String threadNamePrefix;
        private WorkManager workManager;

        public Builder setTlsContextFactory(TlsContextFactory tlsContextFactory)
        {
            this.tlsContextFactory = tlsContextFactory;
            return this;
        }

        public Builder setProxyConfig(ProxyConfig proxyConfig)
        {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public Builder setClientSocketProperties(TcpClientSocketProperties clientSocketProperties)
        {
            this.clientSocketProperties = clientSocketProperties;
            return this;
        }

        public Builder setMaxConnections(int maxConnections)
        {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder setUsePersistentConnections(boolean usePersistentConnections)
        {
            this.usePersistentConnections = usePersistentConnections;
            return this;
        }

        public Builder setConnectionIdleTimeout(int connectionIdleTimeout)
        {
            this.connectionIdleTimeout = connectionIdleTimeout;
            return this;
        }

        public Builder setThreadNamePrefix(String threadNamePrefix)
        {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public Builder setWorkManager(WorkManager workManager)
        {
            this.workManager = workManager;
            return this;
        }

        public GrizzlyHttpClientConfiguration build()
        {
            return new GrizzlyHttpClientConfiguration(tlsContextFactory, proxyConfig, clientSocketProperties, maxConnections,
                                                      usePersistentConnections, connectionIdleTimeout, threadNamePrefix, workManager);
        }
    }
}
