/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * API component to be used as standard SPI discovery mechanism.
 * Locates and returns all registered instances of a given provider
 * class. Advantages of using this interface instead of hitting something
 * like {@link ServiceLoader} directly are the ability to inject a different
 * lookup mechanism and to facilitate testing.
 * <p/>
 * Notice that in future versions of Mule this interface might me moved to
 * a new higher level mule-api artifact
 *
 * @since 3.7.0
 */
public interface ServiceRegistry
{

    /**
     * Searches for implementations of a particular service class
     * using the given class loader.
     *
     * @param providerClass a <code>Class</code>object indicating the
     *                      class or interface of the service providers being detected.
     * @param loader        the class loader to be used to load
     *                      provider-configuration files and instantiate provider classes,
     *                      or <code>null</code> if the system class loader (or, failing that
     *                      the bootstrap class loader) is to be used.
     * @return A {@link Collection} that yields provider objects
     * for the given service, in some arbitrary order.
     */
    <T> Collection<T> lookupProviders(Class<T> providerClass, ClassLoader loader);

    /**
     * Searches for implementations of a particular service class
     *
     * @param providerClass a <code>Class</code>object indicating the
     *                      class or interface of the service providers being detected.
     * @return A {@link Collection} that yields provider objects for the given service, in some arbitrary order.
     */
    <T> Collection<T> lookupProviders(Class<T> providerClass);

}
