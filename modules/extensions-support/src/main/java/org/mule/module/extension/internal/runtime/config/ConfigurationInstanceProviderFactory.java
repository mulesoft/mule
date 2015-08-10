/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import org.mule.api.MuleContext;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * A factory which creates instances of {@link ConfigurationInstanceProvider}
 *
 * @since 4.0
 */
public interface ConfigurationInstanceProviderFactory
{

    /**
     * Creates a new {@link ConfigurationInstanceProvider} which servers instances of a dynamic configuration
     *
     * @param name                the provider's name
     * @param extension           the {@link Extension} that owns the {@link Configuration} that models the configurations instances to be returned
     * @param configuration       the {@link Configuration} that models the configuration instances to be returned
     * @param resolverSet         a {@link ResolverSet} for the configuration's attributes
     * @param extensionManager    the {@link ExtensionManagerAdapter}
     * @param dynamicConfigPolicy a {@link DynamicConfigPolicy} in case the configuration is dynamic
     * @param <T>                 the generic type for the configuration instances to be returned
     * @return a {@link ConfigurationInstanceProvider}
     * @throws Exception if anything goes wrong
     */
    <T> ConfigurationInstanceProvider<T> createDynamicConfigurationInstanceProvider(
            String name,
            Extension extension,
            Configuration configuration,
            ResolverSet resolverSet,
            ExtensionManagerAdapter extensionManager,
            DynamicConfigPolicy dynamicConfigPolicy) throws Exception;


    /**
     * Creates a new {@link ConfigurationInstanceProvider} which servers a static configuration instance
     *
     * @param name             the provider's name
     * @param extension        the {@link Extension} that owns the {@link Configuration} that models the configurations instances to be returned
     * @param configuration    the {@link Configuration} that models the configuration instances to be returned
     * @param resolverSet      a {@link ResolverSet} for the configuration's attributes
     * @param muleContext      the {@link MuleContext} that will own the configuration instances
     * @param extensionManager the {@link ExtensionManagerAdapter}
     * @param <T>              the generic type for the configuration instances to be returned
     * @return a {@link ConfigurationInstanceProvider}
     * @throws Exception if anything goes wrong
     */
    <T> ConfigurationInstanceProvider<T> createStaticConfigurationInstanceProvider(
            String name,
            Extension extension,
            Configuration configuration,
            ResolverSet resolverSet,
            MuleContext muleContext,
            ExtensionManagerAdapter extensionManager) throws Exception;
}
