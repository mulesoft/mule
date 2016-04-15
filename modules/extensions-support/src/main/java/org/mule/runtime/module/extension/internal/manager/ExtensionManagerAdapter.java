/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.runtime.ConfigurationProvider;

import java.util.Optional;

/**
 * Extends the {@link ExtensionManager} interface with contracts
 * which are not to be exposed on the public API
 *
 * @since 4.0
 */
public interface ExtensionManagerAdapter extends ExtensionManager
{

    /**
     * Locates and returns the {@link ConfigurationProvider} which would serve
     * an invocation to the {@link #getConfiguration(ExtensionModel, Object)} method.
     * <p>
     * This means that the returned provider will be located using the same set of rules
     * as the aforementioned method
     *
     * @param extensionModel the {@link ExtensionModel} for which a configuration is wanted
     * @param <C>            the generic type of the configuration instance that the provider returns
     * @return an {@link Optional} {@link ConfigurationProvider}
     */
    <C> Optional<ConfigurationProvider<C>> getConfigurationProvider(ExtensionModel extensionModel);

    /**
     * Locates and returns the {@link ConfigurationProvider} which would serve an invocation to the
     * {@link #getConfiguration(String, Object)} method.
     * <p>
     * This means that the returned provided will be located using the same set of rules
     * as the aforementioned method.
     *
     * @param configurationProviderName the name of a previously registered {@link ConfigurationProvider}
     * @param <C>                       the generic type of the configuration instance that the provider returns
     * @return an {@link Optional} {@link ConfigurationProvider}
     */
    <C> Optional<ConfigurationProvider<C>> getConfigurationProvider(String configurationProviderName);
}
