/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

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
                                                                  ConnectionProviderValueResolver connectionProviderResolver,
                                                                  ExpirationPolicy expirationPolicy,
                                                                  ReflectionCache reflectionCache,
                                                                  ExpressionManager expressionManager,
                                                                  MuleContext muleContext) {

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
                                                                 ConnectionProviderValueResolver connectionProviderResolver,
                                                                 ReflectionCache reflectionCache,
                                                                 ExpressionManager expressionManager,
                                                                 MuleContext muleContext) {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    ClassLoader extensionClassLoader = getClassLoader(extensionModel);
    setContextClassLoader(thread, currentClassLoader, extensionClassLoader);
    try {
      configureConnectionProviderResolver(name, connectionProviderResolver);
      ConfigurationInstance configuration;
      CoreEvent initialiserEvent = null;
      try {
        initialiserEvent = getNullEvent(muleContext);
        initialiseIfNeeded(resolverSet, true, muleContext);
        ConfigurationInstanceFactory configurationFactory = new ConfigurationInstanceFactory(extensionModel,
                                                                                             configurationModel,
                                                                                             resolverSet,
                                                                                             expressionManager,
                                                                                             muleContext);
        configuration = configurationFactory.createConfiguration(name, initialiserEvent, connectionProviderResolver);
      } catch (MuleException e) {
        throw new MuleRuntimeException(new ConfigurationException(createStaticMessage(format("Could not create configuration '%s' for the '%s'",
                                                                                             name,
                                                                                             extensionModel.getName())),
                                                                  e));
      } finally {
        if (initialiserEvent != null) {
          ((BaseEventContext) initialiserEvent.getContext()).success();
        }
      }

      return new ConfigurationProviderToolingAdapter(name, extensionModel, configurationModel, configuration, reflectionCache,
                                                     muleContext);
    } finally {
      setContextClassLoader(thread, extensionClassLoader, currentClassLoader);
    }
  }

  private void configureConnectionProviderResolver(String configName, ValueResolver<ConnectionProvider> resolver) {
    if (resolver instanceof ConnectionProviderResolver) {
      ((ConnectionProviderResolver) resolver).setOwnerConfigName(configName);
    }
  }
}
