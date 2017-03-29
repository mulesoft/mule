/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.api.MuleContext;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;

import com.ning.http.client.AsyncHttpClient;

/**
 * Configuration class to use with a {@link MuleWSDLLocator}.
 */
public class MuleWSDLLocatorConfig
{

    private String baseURI;
    private TlsContextFactory tlsContextFactory;
    private ProxyConfig proxyConfig;
    private MuleContext context;
    private boolean useConnectorToRetrieveWsdl;

    private MuleWSDLLocatorConfig(String baseURI, TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, MuleContext context, boolean useConnectorToRetrieveWsdl)
    {
        this.baseURI = baseURI;
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.context = context;
        this.useConnectorToRetrieveWsdl = useConnectorToRetrieveWsdl;
    }

    public TlsContextFactory getTlsContextFactory()
    {
        return tlsContextFactory;
    }

    public ProxyConfig getProxyConfig()
    {
        return proxyConfig;
    }

    public MuleContext getContext()
    {
        return context;
    }

    public String getBaseURI()
    {
        return baseURI;
    }

    public boolean isUseConnectorToRetrieveWsdl()
    {
        return useConnectorToRetrieveWsdl;
    }

    public static class Builder
    {
        private String baseURI;
        private TlsContextFactory tlsContextFactory;
        private ProxyConfig proxyConfig;
        private MuleContext context;
        private boolean useConnectorToRetrieveWsdl;
        
        public Builder setBaseURI(String baseURI)
        {
            this.baseURI = baseURI;
            return this;
        }

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

        public Builder setContext(MuleContext context)
        {
            this.context = context;
            return this;
        }

        public Builder setUseConnectorToRetrieveWsdl(boolean useConnectorToRetrieveWsdl)
        {
            this.useConnectorToRetrieveWsdl = useConnectorToRetrieveWsdl;
            return this;
        }
        
        public MuleWSDLLocatorConfig build()
        {
            return new MuleWSDLLocatorConfig(baseURI, tlsContextFactory, proxyConfig, context, useConnectorToRetrieveWsdl);
        }
    }
}
