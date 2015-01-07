/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.registry;

import org.mule.api.MuleContext;

import java.util.Properties;

/**
 * Creates {@link ServiceDescriptor} using {@link ServiceDescriptorFactory}
 */
public class MuleTransportServiceDescriptorFactory implements TransportServiceDescriptorFactory
{

    private final String transport;
    private final Properties props;

    public MuleTransportServiceDescriptorFactory(String transport, Properties props)
    {
        this.transport = transport;
        this.props = new Properties();
        this.props.putAll(props);
    }

    @Override
    public ServiceDescriptor create(MuleContext muleContext, Properties overrides) throws ServiceException
    {
        Properties properties = new Properties();
        properties.putAll(props);

        return ServiceDescriptorFactory.create(ServiceType.TRANSPORT, transport, properties, overrides, muleContext, muleContext.getExecutionClassLoader());
    }
}
