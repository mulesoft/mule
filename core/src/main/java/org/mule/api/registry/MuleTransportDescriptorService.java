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

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class MuleTransportDescriptorService implements TransportDescriptorService
{

    private final Map<String, TransportServiceDescriptorFactory> serviceDescriptorFactories = new ConcurrentHashMap<>();

    @Override
    public ServiceDescriptor getDescriptor(String transport, MuleContext muleContext, Properties overrides) throws ServiceException
    {
        TransportServiceDescriptorFactory transportServiceDescriptorFactory = serviceDescriptorFactories.get(transport);

        if (transportServiceDescriptorFactory == null)
        {
            synchronized (serviceDescriptorFactories)
            {
                transportServiceDescriptorFactory = serviceDescriptorFactories.get(transport);

                if (transportServiceDescriptorFactory == null)
                {
                    transportServiceDescriptorFactory = createServiceDescriptorFactory(transport);
                }
            }
        }

        return transportServiceDescriptorFactory.create(muleContext, overrides);
    }

    private TransportServiceDescriptorFactory createServiceDescriptorFactory(String transport) throws ServiceException
    {
        //Stripe off and use the meta-scheme if present
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
