/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.module.launcher.service.ServiceRepository;

/**
 * Registers available {@link Service} in an artifact {@link MuleContext} in order to resolve
 * injectable dependencies.
 */
public class ServicesConfigurationBuilder extends AbstractConfigurationBuilder
{

    private final ServiceRepository serviceRepository;

    /**
     * Creates a new instance.
     *
     * @param serviceRepository repository containing available services. Non null.
     */
    public ServicesConfigurationBuilder(ServiceRepository serviceRepository)
    {
        checkArgument(serviceRepository != null, "serviceRepository cannot be null");
        this.serviceRepository = serviceRepository;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        for (Service service : serviceRepository.getServices())
        {
            muleContext.getRegistry().registerObject(service.getName(), service);
        }
    }
}
