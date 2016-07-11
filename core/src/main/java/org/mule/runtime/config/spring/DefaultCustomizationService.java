/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Optional.ofNullable;
import org.mule.runtime.core.api.CustomService;
import org.mule.runtime.core.api.CustomizationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@inheritDoc}
 */
public class DefaultCustomizationService implements CustomizationService
{
    private Map<String, CustomService> muleContextDefaultServices = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void customizeServiceImpl(String serviceId, T serviceImpl)
    {
        muleContextDefaultServices.put(serviceId, new CustomService(serviceImpl));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void customizeServiceClass(String serviceId, Class<T> serviceClass)
    {
        muleContextDefaultServices.put(serviceId, new CustomService(serviceClass));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CustomService> getCustomizedService(String serviceId)
    {
        return ofNullable(muleContextDefaultServices.get(serviceId));
    }

}
