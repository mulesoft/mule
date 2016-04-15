/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import com.ning.http.client.providers.grizzly.TransportCustomizer;

import java.util.ArrayList;
import java.util.List;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

public class CompositeTransportCustomizer implements TransportCustomizer
{
    private List<TransportCustomizer> transportCustomizers = new ArrayList<>();

    @Override
    public void customize(TCPNIOTransport transport, FilterChainBuilder filterChainBuilder)
    {
        for (TransportCustomizer transportCustomizer : transportCustomizers)
        {
            transportCustomizer.customize(transport, filterChainBuilder);
        }
    }

    public void addTransportCustomizer(TransportCustomizer transportCustomizer)
    {
        transportCustomizers.add(transportCustomizer);
    }
}
