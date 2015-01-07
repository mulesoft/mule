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
 * Creates {@link ServiceDescriptor} for transports
 */
public interface TransportServiceDescriptorFactory
{

    public static final String TRANSPORT_SERVICE_TYPE = "transport";

    /**
     * Creates a {@link ServiceDescriptor}
     *
     * @param muleContext context associated to the created descriptor
     * @param overrides properties used to override default transport properties
     * @return a new {@link ServiceDescriptor}
     * @throws ServiceException when {@link ServiceDescriptor} cannot be created
     */
    ServiceDescriptor create(MuleContext muleContext, Properties overrides) throws ServiceException;
}
