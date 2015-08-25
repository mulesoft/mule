/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.extension.runtime.ConfigurationRegistrationCallback;
import org.mule.extension.runtime.OperationContext;

/**
 * An adapter interface which expands the contract of {@link ExtensionManager} with functionality
 * that is internal to this implementation of the extensions API and that extensions
 * themselves shouldn't be able to access
 *
 * @since 3.7.0
 */
public interface ExtensionManagerAdapter extends ExtensionManager, ConfigurationRegistrationCallback
{

    /**
     * Returns a configuration instance obtained through a {@link ConfigurationProvider}
     * previously registered using the {@link #registerConfigurationProvider(ExtensionModel, String, ConfigurationProvider)}
     * under the given {@code configurationProviderName}.
     * <p/>
     * After the {@link ConfigurationProvider} has been located, an instance is returned by
     * invoking its {@link ConfigurationProvider#get(OperationContext)} with {@code operationContext}
     * as the argument.
     *
     * @param extensionModel            the {@link ExtensionModel} that owns the {@link ConfigurationModel}
     * @param configurationProviderName the name of a previously registered {@link ConfigurationProvider}
     * @param operationContext          a {@link OperationContext}
     * @param <C>                       the type of the configuration instance to be returned
     * @return a configuration instance
     */
    <C> C getConfiguration(ExtensionModel extensionModel, String configurationProviderName, OperationContext operationContext);

    <C> C getConfiguration(ExtensionModel extensionModel, OperationContext operationContext);

}
