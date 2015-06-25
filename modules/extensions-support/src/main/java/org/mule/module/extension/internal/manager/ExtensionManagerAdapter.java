/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.ExtensionManager;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;

/**
 * An adapter interface which expands the contract
 * of {@link ExtensionManager} with functionality
 * that is internal to this implementation
 * of the extensions API and that extensions
 * themselves shouldn't be able to access
 *
 * @since 3.7.0
 */
public interface ExtensionManagerAdapter extends ExtensionManager
{

    /**
     * Returns a configuration instance obtained through the given
     * {@code configurationInstanceProvider} and for the
     * provided {@code operationContext}. This method will fail
     * if {@code configurationInstanceProvider} hasn't previously been
     * registered through the {@link #registerConfigurationInstanceProvider(String, ConfigurationInstanceProvider)}
     * method
     *
     * @param configurationInstanceProvider a registered {@link ConfigurationInstanceProvider}
     * @param operationContext              a {@link OperationContext}
     * @param <C>                           the type of the configuration instance to be returned
     * @return a configuration instance
     */
    <C> C getConfigurationInstance(ConfigurationInstanceProvider<C> configurationInstanceProvider, OperationContext operationContext);
}
