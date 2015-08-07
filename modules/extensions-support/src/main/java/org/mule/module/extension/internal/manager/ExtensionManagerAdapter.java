/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;

/**
 * An adapter interface which expands the contract of {@link ExtensionManager} with functionality
 * that is internal to this implementation of the extensions API and that extensions
 * themselves shouldn't be able to access
 *
 * @since 3.7.0
 */
public interface ExtensionManagerAdapter extends ExtensionManager, ConfigurationInstanceRegistrationCallback
{

    /**
     * Returns a configuration instance obtained through a {@link ConfigurationInstanceProvider}
     * previously registered using the {@link #registerConfigurationInstanceProvider(Extension, String, ConfigurationInstanceProvider)}
     * under the given {@code configurationInstanceProviderName}.
     * <p/>
     * After the {@link ConfigurationInstanceProvider} has been located, an instance is returned by
     * invoking its {@link ConfigurationInstanceProvider#get(OperationContext)} with {@code operationContext}
     * as the argument.
     *
     * @param extension                         the {@link Extension} that owns the {@link Configuration}
     * @param configurationInstanceProviderName the name of a previously registered {@link ConfigurationInstanceProvider}
     * @param operationContext                  a {@link OperationContext}
     * @param <C>                               the type of the configuration instance to be returned
     * @return a configuration instance
     */
    <C> C getConfigurationInstance(Extension extension, String configurationInstanceProviderName, OperationContext operationContext);

    <C> C getConfigurationInstance(Extension extension, OperationContext operationContext);

}
