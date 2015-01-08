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
 * Creates {@link org.mule.api.registry.ServiceDescriptor} for registered transports.
 */
public interface TransportDescriptorService
{

    /**
     * Provides a service descriptor for a given transport
     *
     * @param transport name of the transport
     * @param muleContext context associated with the descriptor
     * @param overrides properties used to override the default transport properties
     * @return a {@link ServiceDescriptor} for the transport using overridden properties
     * @throws ServiceException
     */
    ServiceDescriptor getDescriptor(String transport, MuleContext muleContext, Properties overrides) throws ServiceException;
}
