/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.withExtensionClassLoader;

import static java.lang.String.format;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.metadata.internal.MuleMetadataService;
import org.mule.runtime.module.extension.api.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import jakarta.inject.Inject;

/**
 * Default implementation of {@link ConfigurationProviderFactory}
 *
 * @since 4.0
 */
public final class DefaultConfigurationProviderFactory implements ConfigurationProviderFactory {

  @Inject
  private ReflectionCache reflectionCache;
  @Inject
  private ExpressionManager expressionManager;
  @Inject
  private MuleContext muleContext;

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationProvider createDynamicConfigurationProvider(String name,
                                                                  ExtensionModel extensionModel,
                                                                  ConfigurationModel configurationModel,
                                                                  ResolverSet resolverSet,
                                                                  ConnectionProviderValueResolver connectionProviderResolver,
                                                                  ExpirationPolicy expirationPolicy) {

    configureConnectionProviderResolver(name, connectionProviderResolver);
    return new DynamicConfigurationProvider(name, extensionModel, configurationModel, resolverSet, connectionProviderResolver,
                                            expirationPolicy, reflectionCache, expressionManager, muleContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationProvider createStaticConfigurationProvider(String name,
                                                                 ExtensionModel extensionModel,
                                                                 ConfigurationModel configurationModel,
                                                                 ResolverSet resolverSet,
                                                                 ConnectionProviderValueResolver connectionProviderResolver)
      throws Exception {
    return withExtensionClassLoader(extensionModel, () -> {
      configureConnectionProviderResolver(name, connectionProviderResolver);
      ConfigurationInstance configuration;
      CoreEvent initialiserEvent = null;
      try {
        initialiserEvent = getInitialiserEvent(muleContext);
        initialiseIfNeeded(resolverSet, true, muleContext);
        ConfigurationInstanceFactory configurationFactory = new ConfigurationInstanceFactory(extensionModel,
                                                                                             configurationModel,
                                                                                             resolverSet,
                                                                                             expressionManager,
                                                                                             muleContext);
        configuration = configurationFactory.createConfiguration(name, initialiserEvent, connectionProviderResolver);
      } catch (MuleException e) {
        throw new ConfigurationException(createStaticMessage(format("Could not create configuration '%s' for the '%s'", name,
                                                                    extensionModel.getName())),
                                         e);
      } finally {
        if (initialiserEvent != null) {
          ((BaseEventContext) initialiserEvent.getContext()).success();
        }
      }

      DefaultRegistry registry = new DefaultRegistry(muleContext);
      return registry.<MuleMetadataService>lookupByType(MuleMetadataService.class)
          .map(metadataService -> (StaticConfigurationProvider) new ConfigurationProviderToolingAdapter(name,
                                                                                                        extensionModel,
                                                                                                        configurationModel,
                                                                                                        configuration,
                                                                                                        metadataService,
                                                                                                        registry
                                                                                                            .<ConnectionManager>lookupByName(OBJECT_CONNECTION_MANAGER)
                                                                                                            .get(),
                                                                                                        reflectionCache,
                                                                                                        muleContext))
          .orElseGet(() -> new StaticConfigurationProvider(name, extensionModel,
                                                           configurationModel, configuration,
                                                           muleContext));
    });
  }

  private void configureConnectionProviderResolver(String configName, ValueResolver<ConnectionProvider> resolver) {
    if (resolver instanceof ConnectionProviderResolver) {
      ((ConnectionProviderResolver) resolver).setOwnerConfigName(configName);
    }
  }
}
