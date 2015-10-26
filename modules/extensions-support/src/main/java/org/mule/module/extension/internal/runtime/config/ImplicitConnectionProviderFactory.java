/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import org.mule.api.MuleEvent;
import org.mule.extension.api.connection.ConnectionProvider;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.module.extension.internal.introspection.ImplicitObjectUtils;

import java.util.List;

/**
 * Creates {@link ConnectionProvider} instances which can be implicitly derived from a given
 * {@link ExtensionModel}.
 *
 * @since 4.0
 */
public interface ImplicitConnectionProviderFactory
{

    /**
     * Creates a new {@link ConnectionProvider} based on the {@link ConnectionProviderModel} obtained
     * from invoking {@link ImplicitObjectUtils#getFirstImplicit(List)} on the {@code extensionModel}'s providers.
     *
     * @param configName the name of the configuration that will own the returned {@link ConnectionProvider}
     * @param extensionModel the model that represents the owning extension
     * @param event the {@link MuleEvent} that will be used to evaluate any default parameters that requires resolving an expression
     * @param <Config> the generic type of the config types that the returned provider accepts
     * @param <Connector> the generic type of the connections that the returned provider produces
     * @return a {@link ConnectionProvider}
     * @throws IllegalArgumentException if the {@code extensionModel} doesn't have any {@link ConnectionProviderModel} which can be used implicitly
     */
    <Config, Connector> ConnectionProvider<Config, Connector> createImplicitConnectionProvider(String configName, ExtensionModel extensionModel, MuleEvent event);
}
