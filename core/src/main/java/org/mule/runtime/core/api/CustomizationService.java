/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import java.util.Optional;

/**
 * Interface that allows to customize the set of services provided by the {@code MuleContext}.
 *
 * It's possible to add new services or replace default implementation for services specifying
 * a service implementation or a services class.
 *
 * For replacing an existent service, the service identifier must be used which can be located
 * on {@link org.mule.runtime.core.api.config.MuleProperties} class.
 *
 * @since 4.0
 */
public interface CustomizationService
{

    /**
     * Allows to customize a service. The provided implementation will be used instead of the
     * default one if it's replacing an existent service.
     *
     * The service implementation can be annotated with @Inject and implement methods from
     * {@link  org.mule.runtime.core.api.lifecycle.Lifecycle}.
     *
     * The service identifier can be used to locate the service in the mule registry.
     *
     * @param serviceId identifier of the services implementation to customize.
     * @param serviceImpl the service implementation instance
     * @param <T> the service type
     */
    <T> void customizeServiceImpl(String serviceId, T serviceImpl);

    /**
     * Allows to customize a service. The provided class will be used to instantiate the service
     * that replaces the default one if it's replacing an existent service.
     *
     * The service class can be annotated with @Inject and implement methods from
     * {@link  org.mule.runtime.core.api.lifecycle.Lifecycle}.
     *
     * @param serviceId identifier of the services implementation to customize.
     * @param serviceClass the service class
     * @param <T> the service type
     */
    <T> void customizeServiceClass(String serviceId, Class<T> serviceClass);

    /**
     * Provides the configuration of a particular service.
     *
     * @param serviceId identifier of the service.
     * @return the service definition
     */
    Optional<CustomService> getCustomizedService(String serviceId);

}
