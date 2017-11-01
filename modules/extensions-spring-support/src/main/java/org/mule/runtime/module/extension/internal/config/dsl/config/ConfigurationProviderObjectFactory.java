/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ImplicitConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticConnectionProviderResolver;

import java.util.Optional;

import javax.inject.Inject;

/**
 * A {@link AbstractExtensionObjectFactory} which produces {@link ConfigurationProvider} instances
 *
 * @since 4.0
 */
class ConfigurationProviderObjectFactory extends AbstractExtensionObjectFactory<ConfigurationProvider>
    implements ObjectFactory<ConfigurationProvider> {

  private final String name;
  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final ConfigurationProviderFactory configurationProviderFactory = new DefaultConfigurationProviderFactory();

  private ExpirationPolicy expirationPolicy;
  private Optional<ConnectionProviderValueResolver> connectionProviderResolver = empty();
  private ConfigurationProvider instance;
  private boolean requiresConnection = false;

  ConfigurationProviderObjectFactory(String name,
                                     ExtensionModel extensionModel,
                                     ConfigurationModel configurationModel,
                                     MuleContext muleContext) {
    super(muleContext);
    this.name = name;
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
  }

  @Override
  public ConfigurationProvider doGetObject() throws Exception {
    if (instance == null) {
      instance = createInnerInstance();
    }
    return instance;
  }

  private ConfigurationProvider createInnerInstance() throws ConfigurationException {
    if (expirationPolicy == null) {
      expirationPolicy = muleContext.getConfiguration().getDynamicConfigExpiration().getExpirationPolicy();
    }

    ResolverSet resolverSet = getParametersResolver().getParametersAsHashedResolverSet(configurationModel, muleContext);
    final ConnectionProviderValueResolver connectionProviderResolver = getConnectionProviderResolver();
    connectionProviderResolver.getResolverSet()
        .ifPresent((CheckedConsumer) resolver -> initialiseIfNeeded(resolver, true, muleContext));

    ConfigurationProvider configurationProvider;
    try {
      if (resolverSet.isDynamic() || connectionProviderResolver.isDynamic()) {
        configurationProvider =
            configurationProviderFactory.createDynamicConfigurationProvider(name, extensionModel,
                                                                            configurationModel,
                                                                            resolverSet,
                                                                            connectionProviderResolver,
                                                                            expirationPolicy,
                                                                            muleContext);
      } else {
        configurationProvider = configurationProviderFactory
            .createStaticConfigurationProvider(name,
                                               extensionModel,
                                               configurationModel,
                                               resolverSet,
                                               connectionProviderResolver,
                                               muleContext);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return configurationProvider;
  }

  private ConnectionProviderValueResolver getConnectionProviderResolver() {
    return connectionProviderResolver.orElseGet(() -> {
      if (requiresConnection) {
        return new ImplicitConnectionProviderValueResolver(name, extensionModel, configurationModel, muleContext);
      }
      return new StaticConnectionProviderResolver(null, null);
    });
  }

  public void setExpirationPolicy(ExpirationPolicy expirationPolicy) {
    this.expirationPolicy = expirationPolicy;
  }

  public void setConnectionProviderResolver(ConnectionProviderResolver connectionProviderResolver) {
    this.connectionProviderResolver = ofNullable(connectionProviderResolver);
  }

  public void setRequiresConnection(boolean requiresConnection) {
    this.requiresConnection = requiresConnection;
  }
}
