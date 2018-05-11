/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.tcp.TcpClientSocketProperties;


public class HttpClientConfiguration
{
    private final TlsContextFactory tlsContextFactory;
    private final TlsContextFactory defaultTlsContextFactory;
    private final ProxyConfig proxyConfig;
    private final TcpClientSocketProperties clientSocketProperties;
    private final int maxConnections;
    private final boolean usePersistentConnections;
    private final int connectionIdleTimeout;
    private final boolean streaming;
    private final int responseBufferSize;
    private final String threadNamePrefix;
    private final String ownerName;
    private final Integer maxWorkerPoolSize;
    private final Integer workerCoreSize;
    private final Integer maxKernelPoolSize;
    private final Integer kernelCoreSize;
    private final Integer selectorRunnersCount;

    private HttpClientConfiguration(TlsContextFactory tlsContextFactory, TlsContextFactory defaultTlsContextFactory,
                                    ProxyConfig proxyConfig,
                                    TcpClientSocketProperties clientSocketProperties,
                                    int maxConnections, boolean usePersistentConnections, int connectionIdleTimeout,
                                    boolean streaming, int responseBufferSize, String threadNamePrefix, String ownerName,
                                    Integer maxWorkerPoolSize, Integer workerCoreSize, Integer maxKernelPoolSize,
                                    Integer kernelCoreSize, Integer selectorRunnersCount)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.defaultTlsContextFactory = defaultTlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.clientSocketProperties = clientSocketProperties;
        this.maxConnections = maxConnections;
        this.usePersistentConnections = usePersistentConnections;
        this.connectionIdleTimeout = connectionIdleTimeout;
        this.streaming = streaming;
        this.responseBufferSize = responseBufferSize;
        this.threadNamePrefix = threadNamePrefix;
        this.ownerName = ownerName;
        this.maxWorkerPoolSize = maxWorkerPoolSize;
        this.workerCoreSize = workerCoreSize;
        this.maxKernelPoolSize = maxKernelPoolSize;
        this.kernelCoreSize = kernelCoreSize;
        this.selectorRunnersCount = selectorRunnersCount;
    }

    public TlsContextFactory getTlsContextFactory()
    {
        return tlsContextFactory;
    }

    public TlsContextFactory getDefaultTlsContextFactory()
    {
        return defaultTlsContextFactory;
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

    public boolean isStreaming()
    {
        return streaming;
    }

    public int getResponseBufferSize() {
        return responseBufferSize;
    }

    public String getThreadNamePrefix()
    {
        return threadNamePrefix;
    }

    public String getOwnerName()
    {
        return ownerName;
    }

    public Integer getMaxWorkerPoolSize()
    {
        return maxWorkerPoolSize;
    }

    public Integer getWorkerCoreSize()
    {
        return workerCoreSize;
    }

    public Integer getMaxKernelPoolSize()
    {
        return maxKernelPoolSize;
    }

    public Integer getKernelCoreSize()
    {
        return kernelCoreSize;
    }

    public Integer getSelectorRunnersCount()
    {
        return selectorRunnersCount;
    }

    public static class Builder
    {
        private TlsContextFactory tlsContextFactory;
        private TlsContextFactory defaultTlsContextFactory;
        private ProxyConfig proxyConfig;
        private TcpClientSocketProperties clientSocketProperties;
        private int maxConnections;
        private boolean usePersistentConnections;
        private int connectionIdleTimeout;
        private boolean streaming;
        private int responseBufferSize;
        private String threadNamePrefix;
        private String ownerName;
        private Integer maxWorkerPoolSize;
        private Integer workerCoreSize;
        private Integer maxKernelPoolSize;
        private Integer kernelCoreSize;
        private Integer selectorRunnersCount;

        public Builder setTlsContextFactory(TlsContextFactory tlsContextFactory)
        {
            this.tlsContextFactory = tlsContextFactory;
            return this;
        }

        public Builder setDefaultTlsContextFactory(TlsContextFactory defaultTlsContextFactory)
        {
            this.defaultTlsContextFactory = defaultTlsContextFactory;
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

        public Builder setStreaming(boolean streaming)
        {
            this.streaming = streaming;
            return this;
        }

        public Builder setResponseBufferSize(int responseBufferSize) {
            this.responseBufferSize = responseBufferSize;
            return this;
        }

        public Builder setThreadNamePrefix(String threadNamePrefix)
        {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public Builder setOwnerName(String ownerName)
        {
            this.ownerName = ownerName;
            return this;
        }

        public Builder setMaxWorkerPoolSize(Integer maxWorkerPoolSize)
        {
            this.maxWorkerPoolSize = maxWorkerPoolSize;
            return this;
        }

        public Builder setWorkerCoreSize(Integer workerCoreSize)
        {
            this.workerCoreSize = workerCoreSize;
            return this;
        }

        public Builder setMaxKernelPoolSize(Integer maxKernelPoolSize)
        {
            this.maxKernelPoolSize = maxKernelPoolSize;
            return this;
        }

        public Builder setKernelCoreSize(Integer kernelCoreSize)
        {
            this.kernelCoreSize = kernelCoreSize;
            return this;
        }

        public Builder setSelectorRunnersCount(Integer selectorRunnersCount)
        {
            this.selectorRunnersCount = selectorRunnersCount;
            return this;
        }

        public HttpClientConfiguration build()
        {
            return new HttpClientConfiguration(tlsContextFactory, defaultTlsContextFactory, proxyConfig, clientSocketProperties, maxConnections,
                    usePersistentConnections, connectionIdleTimeout, streaming, responseBufferSize, threadNamePrefix, ownerName,
                    maxWorkerPoolSize, workerCoreSize, maxKernelPoolSize, kernelCoreSize, selectorRunnersCount);
        }
    }
}
