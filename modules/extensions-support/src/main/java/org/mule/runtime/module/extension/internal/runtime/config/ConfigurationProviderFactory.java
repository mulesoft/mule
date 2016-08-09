/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * A factory which creates instances of {@link ConfigurationProvider}
 *
 * @since 4.0
 */
public interface ConfigurationProviderFactory {

  /**
   * Creates a new {@link ConfigurationProvider} which servers instances of a dynamic configuration
   *
   * @param name the provider's name
   * @param configurationModel the {@link RuntimeConfigurationModel} that describes the configuration instances to be returned
   * @param resolverSet a {@link ResolverSet} for the configuration's attributes
   * @param connectionProviderResolver A {@link ValueResolver} to obtain a {@link ConnectionProvider}
   * @param dynamicConfigPolicy a {@link DynamicConfigPolicy} in case the configuration is dynamic
   * @param <T> the generic type for the configuration instances to be returned
   * @return a {@link ConfigurationProvider}
   * @throws Exception if anything goes wrong
   */
  <T> ConfigurationProvider<T> createDynamicConfigurationProvider(String name, RuntimeConfigurationModel configurationModel,
                                                                  ResolverSet resolverSet,
                                                                  ValueResolver<ConnectionProvider> connectionProviderResolver,
                                                                  DynamicConfigPolicy dynamicConfigPolicy)
      throws Exception;


  /**
   * Creates a new {@link ConfigurationProvider} which servers a static configuration instance
   *
   * @param name the provider's name
   * @param configurationModel the {@link RuntimeConfigurationModel} that describes the configuration instances to be returned
   * @param resolverSet a {@link ResolverSet} for the configuration's attributes
   * @param connectionProviderResolver A {@link ValueResolver} to obtain a {@link ConnectionProvider}
   * @param muleContext the {@link MuleContext} that will own the configuration instances
   * @param <T> the generic type for the configuration instances to be returned
   * @return a {@link ConfigurationProvider}
   * @throws Exception if anything goes wrong
   */
  <T> ConfigurationProvider<T> createStaticConfigurationProvider(String name, RuntimeConfigurationModel configurationModel,
                                                                 ResolverSet resolverSet,
                                                                 ValueResolver<ConnectionProvider> connectionProviderResolver,
                                                                 MuleContext muleContext)
      throws Exception;
}
