/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.withExtensionClassLoader;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
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
  public ConfigurationProvider createDynamicConfigurationProvider(String name,
                                                                  ExtensionModel extensionModel,
                                                                  ConfigurationModel configurationModel,
                                                                  ResolverSet resolverSet,
                                                                  ValueResolver<ConnectionProvider> connectionProviderResolver,
                                                                  DynamicConfigPolicy dynamicConfigPolicy)
      throws Exception {
    configureConnectionProviderResolver(name, connectionProviderResolver);
    return new DynamicConfigurationProvider(name, extensionModel, configurationModel, resolverSet, connectionProviderResolver,
                                            dynamicConfigPolicy.getExpirationPolicy());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationProvider createStaticConfigurationProvider(String name,
                                                                 ExtensionModel extensionModel,
                                                                 ConfigurationModel configurationModel,
                                                                 ResolverSet resolverSet,
                                                                 ValueResolver<ConnectionProvider> connectionProviderResolver,
                                                                 MuleContext muleContext)
      throws Exception {
    return withExtensionClassLoader(extensionModel, () -> {
      configureConnectionProviderResolver(name, connectionProviderResolver);
      ConfigurationInstance configuration;
      try {
        configuration = new ConfigurationInstanceFactory(extensionModel, configurationModel, resolverSet)
            .createConfiguration(name, getInitialiserEvent(muleContext), connectionProviderResolver);
      } catch (MuleException e) {
        throw new ConfigurationException(createStaticMessage(String
            .format("Could not create configuration '%s' for the '%s'", name, extensionModel.getName())),
                                         e);
      }

      return new ConfigurationProviderMetadataAdapter(name, extensionModel, configurationModel, configuration);
    });
  }

  private void configureConnectionProviderResolver(String configName, ValueResolver<ConnectionProvider> resolver) {
    if (resolver instanceof ConnectionProviderResolver) {
      ((ConnectionProviderResolver) resolver).setOwnerConfigName(configName);
    }
  }
}
