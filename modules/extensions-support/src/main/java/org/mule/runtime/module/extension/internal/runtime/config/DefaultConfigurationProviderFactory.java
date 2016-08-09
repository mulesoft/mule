/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.withExtensionClassLoader;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * Default implementation of {@link ConfigurationProviderFactory}
 *
 * @since 4.0
 */
public final class DefaultConfigurationProviderFactory implements ConfigurationProviderFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> ConfigurationProvider<T> createDynamicConfigurationProvider(String name,
                                                                         RuntimeConfigurationModel configurationModel,
                                                                         ResolverSet resolverSet,
                                                                         ValueResolver<ConnectionProvider> connectionProviderResolver,
                                                                         DynamicConfigPolicy dynamicConfigPolicy)
      throws Exception {
    configureConnectionProviderResolver(name, connectionProviderResolver);
    return new DynamicConfigurationProvider<>(name, configurationModel, resolverSet, connectionProviderResolver,
                                              dynamicConfigPolicy.getExpirationPolicy());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> ConfigurationProvider<T> createStaticConfigurationProvider(String name, RuntimeConfigurationModel configurationModel,
                                                                        ResolverSet resolverSet,
                                                                        ValueResolver<ConnectionProvider> connectionProviderResolver,
                                                                        MuleContext muleContext)
      throws Exception {
    return withExtensionClassLoader(configurationModel.getExtensionModel(), () -> {
      configureConnectionProviderResolver(name, connectionProviderResolver);
      ConfigurationInstance<T> configuration;
      try {
        configuration = new ConfigurationInstanceFactory<T>(configurationModel, resolverSet)
            .createConfiguration(name, getInitialiserEvent(muleContext), connectionProviderResolver);
      } catch (MuleException e) {
        throw new ConfigurationException(createStaticMessage(String
            .format("Could not create configuration '%s' for the '%s'", name, configurationModel.getExtensionModel().getName())),
                                         e);
      }

      return new StaticConfigurationProvider<>(name, configurationModel, configuration);
    });
  }

  private void configureConnectionProviderResolver(String configName, ValueResolver<ConnectionProvider> resolver) {
    if (resolver instanceof ConnectionProviderResolver) {
      ((ConnectionProviderResolver) resolver).setOwnerConfigName(configName);
    }
  }
}
