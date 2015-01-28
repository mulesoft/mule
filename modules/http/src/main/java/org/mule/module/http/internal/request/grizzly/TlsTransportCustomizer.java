/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import org.mule.transport.ssl.api.TlsContextFactory;

import com.ning.http.client.providers.grizzly.TransportCustomizer;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.ssl.SSLFilter;

/**
 * Transport customizer that sets enabled cipher suites and protocols in the SSL filter.
 */
public class TlsTransportCustomizer implements TransportCustomizer
{

    private final TlsContextFactory tlsContextFactory;

    public TlsTransportCustomizer(TlsContextFactory tlsContextFactory)
    {
        this.tlsContextFactory = tlsContextFactory;
    }

    @Override
    public void customize(TCPNIOTransport transport, FilterChainBuilder filterChainBuilder)
    {
        int index = filterChainBuilder.indexOfType(SSLFilter.class);

        if (index == -1)
        {
            return;
        }

        SSLFilter sslFilter = (SSLFilter) filterChainBuilder.get(index);
        String[] enabledCipherSuites = tlsContextFactory.getEnabledCipherSuites();
        String[] enabledProtocols = tlsContextFactory.getEnabledProtocols();

        if (enabledCipherSuites != null)
        {
            sslFilter.getClientSSLEngineConfigurator().setEnabledCipherSuites(enabledCipherSuites);
        }
        if (enabledProtocols != null)
        {
            sslFilter.getClientSSLEngineConfigurator().setEnabledProtocols(enabledProtocols);
        }
    }
}
