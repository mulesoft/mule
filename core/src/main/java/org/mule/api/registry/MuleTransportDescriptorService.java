/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.registry;

import org.mule.api.MuleContext;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.SpiUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class MuleTransportDescriptorService implements TransportDescriptorService
{

    private final LoadingCache<String, TransportServiceDescriptorFactory> serviceDescriptorFactories = CacheBuilder.newBuilder().build(new CacheLoader<String, TransportServiceDescriptorFactory>()
    {
        @Override
        public TransportServiceDescriptorFactory load(String transport) throws Exception
        {
            return createServiceDescriptorFactory(transport);
        }
    });

    @Override
    public ServiceDescriptor getDescriptor(String transport, MuleContext muleContext, Properties overrides) throws ServiceException
    {
        try
        {
            TransportServiceDescriptorFactory transportServiceDescriptorFactory = serviceDescriptorFactories.get(transport);

            return transportServiceDescriptorFactory.create(muleContext, overrides);
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof ServiceException)
            {
                throw (ServiceException) e.getCause();
            }
            else
            {
                throw new ServiceException(MessageFactory.createStaticMessage("Cannot create service descriptor"), e);
            }
        }
    }

    private TransportServiceDescriptorFactory createServiceDescriptorFactory(String transport) throws ServiceException
    {
        // Strips off and use the meta-scheme if present
        String scheme = transport;
        if (scheme.contains(":"))
        {
            scheme = transport.substring(0, transport.indexOf(":"));
        }

        Properties serviceDescriptor = SpiUtils.findServiceDescriptor(ServiceType.TRANSPORT, scheme);
        if (serviceDescriptor == null)
        {
            throw new ServiceException(MessageFactory.createStaticMessage(String.format("Unable to obtain a service descriptor for transport '%s'", transport)));
        }

        TransportServiceDescriptorFactory transportServiceDescriptorFactory = new MuleTransportServiceDescriptorFactory(transport, serviceDescriptor);
        serviceDescriptorFactories.put(transport, transportServiceDescriptorFactory);

        return transportServiceDescriptorFactory;
    }
}
